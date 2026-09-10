package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.exception.ValidationException;
import com.example.demo.modelo.Rol;
import com.example.demo.modelo.Usuario;
import com.example.demo.repository.RolRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuditoriaContexto auditoriaContexto;

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RolRepository rolRepository;
    private final EmailService emailService;

    // RN-CU00-03 / RN-GLOBAL-007: máximo de intentos antes del bloqueo temporal
    private static final int MAX_INTENTOS_FALLIDOS = 5;
    private static final long MINUTOS_BLOQUEO = 15;

    public VerifyDpiResponseDTO verificarDpi(VerifyDpiDTO dto) {
        var usuarioOpt = usuarioRepository.findByDpi(dto.getDpi());

        if (usuarioOpt.isEmpty()) {
            // FA03 - No registrado
            return new VerifyDpiResponseDTO(false, false,
                    "No se encontró un registro asociado a este DPI. Será redirigido al formulario de registro.");
        }

        Usuario usuario = usuarioOpt.get();
        boolean esPaciente = "Paciente".equalsIgnoreCase(usuario.getRol().getNombre());

        if (!esPaciente) {
            // FA04 - DPI pertenece a usuario interno
            return new VerifyDpiResponseDTO(true, false,
                    "Este DPI pertenece a un usuario del sistema interno. Por favor, contacte a recepción.");
        }

        // RN-CU00-01 - Registrado y es paciente
        return new VerifyDpiResponseDTO(true, true,
                "Bienvenido(a), " + usuario.getNombreCompleto() + ". Será redirigido al formulario de agendamiento de cita.");
    }

    // CORREGIDO: sin noRollbackFor, cada intento fallido guardaba el
    // incremento de intentosFallidos pero la propia ValidationException
    // (RuntimeException) lanzada justo después disparaba el rollback
    // automático de Spring, deshaciendo ese save(). Por eso el contador
    // siempre volvía a leer 0 desde la BD y se quedaba pegado en
    // "Intentos restantes: 4" sin nunca bajar a 3, 2, 1 ni bloquear la
    // cuenta. Con noRollbackFor, el guardado del contador (y del bloqueo)
    // sí se confirma aunque el método termine lanzando la excepción.
    @Transactional(noRollbackFor = ValidationException.class)
    public LoginResponseDTO login(LoginRequestDTO dto) {
        auditoriaContexto.aplicar();
        Usuario usuario = usuarioRepository.findByNombreUsuario(dto.getNombreUsuario())
                .orElseThrow(() -> new ValidationException("Usuario o contraseña incorrectos."));

        // FA07 - Cuenta actualmente bloqueada: no evaluamos ni la contraseña
        if (usuario.getBloqueadoHasta() != null && usuario.getBloqueadoHasta().isAfter(LocalDateTime.now())) {
            throw new ValidationException("Cuenta bloqueada temporalmente. Intente de nuevo en 15 minutos.");
        }

        if (!passwordEncoder.matches(dto.getPassword(), usuario.getPasswordHash())) {
            // FA06 - Credenciales incorrectas (RN-CU00-03 / RN-GLOBAL-007)
            int intentos = (usuario.getIntentosFallidos() == null ? 0 : usuario.getIntentosFallidos()) + 1;

            if (intentos >= MAX_INTENTOS_FALLIDOS) {
                usuario.setIntentosFallidos((short) 0);
                usuario.setBloqueadoHasta(LocalDateTime.now().plusMinutes(MINUTOS_BLOQUEO));
                usuarioRepository.save(usuario);
                throw new ValidationException("Cuenta bloqueada temporalmente. Intente de nuevo en 15 minutos.");
            }

            usuario.setIntentosFallidos((short) intentos);
            usuarioRepository.save(usuario);
            int restantes = MAX_INTENTOS_FALLIDOS - intentos;
            throw new ValidationException(
                    "Usuario o contraseña incorrectos. Intentos restantes: " + restantes + ".");
        }

        if (usuario.getEstado() == 0) {
            throw new ValidationException("Su cuenta se encuentra inactiva. Contacte al administrador.");
        }

        // Login correcto: reinicia el contador de intentos fallidos si tenía alguno
        if ((usuario.getIntentosFallidos() != null && usuario.getIntentosFallidos() > 0)
                || usuario.getBloqueadoHasta() != null) {
            usuario.setIntentosFallidos((short) 0);
            usuario.setBloqueadoHasta(null);
            usuarioRepository.save(usuario);
        }

        Integer sucursalId = usuario.getSucursal() != null ? usuario.getSucursal().getId() : null;

        String token = jwtUtil.generarToken(usuario.getId(), usuario.getNombreUsuario(), usuario.getRol().getNombre(), sucursalId);

        // El id también viaja embebido en el propio JWT (claim "id"), que es
        // lo que ahora usan CU-06/CU-07/CU-08/CU-11 vía @AuthenticationPrincipal.
        // Se sigue devolviendo aquí también porque el front lo sigue usando
        // para mostrar/loggear el usuario en pantalla.
        // sucursalId/sucursalNombre viajan también fuera del token para que el
        // panel del Administrador de Sede pueda mostrar "Sede Central" en el
        // header sin tener que decodificar el JWT en el navegador.
        return new LoginResponseDTO(
                usuario.getId(),
                token,
                usuario.getNombreUsuario(),
                usuario.getNombreCompleto(),
                usuario.getRol().getNombre(),
                sucursalId,
                usuario.getSucursal() != null ? usuario.getSucursal().getNombre() : null
        );
    }

    // CU-02: Registro de Usuarios Externos

    @Transactional
    public RegistroPacienteResponseDTO registrarPaciente(RegistroPacienteDTO dto) {
        auditoriaContexto.aplicar();

        // FA02 - DPI ya registrado
        if (usuarioRepository.findByDpi(dto.getDpi()).isPresent()) {
            throw new ValidationException(
                    "Ya existe una cuenta registrada con este número de DPI. Si ya tiene cuenta, inicie sesión.");
        }
        // FA03 - Correo ya registrado
        if (usuarioRepository.existsByCorreoElectronico(dto.getCorreoElectronico())) {
            throw new ValidationException("Ya existe una cuenta registrada con este correo electrónico.");
        }
        // RN-CU02-05 - Nombre de usuario único
        if (usuarioRepository.existsByNombreUsuario(dto.getNombreUsuario())) {
            throw new ValidationException(
                    "El nombre de usuario " + dto.getNombreUsuario() + " ya se encuentra registrado.");
        }

        Rol rolPaciente = rolRepository.findAll().stream()
                .filter(r -> "Paciente".equalsIgnoreCase(r.getNombre()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "El rol 'Paciente' no está configurado en el catálogo de roles."));

        Usuario usuario = new Usuario();
        usuario.setNombreCompleto(dto.getNombreCompleto());
        usuario.setCorreoElectronico(dto.getCorreoElectronico());
        usuario.setNombreUsuario(dto.getNombreUsuario());
        usuario.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        usuario.setDpi(dto.getDpi());
        usuario.setNit(dto.getNit());
        usuario.setTelefono(dto.getTelefono());
        usuario.setNumeroSeguro(dto.getNumeroSeguro());
        usuario.setRol(rolPaciente);
        usuario.setSucursal(null);
        usuario.setEspecialidad(null);
        usuario.setEstado((short) 1);

        usuarioRepository.save(usuario);

        // correo de bienvenida (RN-GLOBAL-006)
        emailService.enviarBienvenida(usuario);

        return new RegistroPacienteResponseDTO(
                "¡Registro exitoso! Su cuenta ha sido creada. Ahora puede iniciar sesión con sus credenciales.",
                usuario.getNombreUsuario()
        );
    }
}