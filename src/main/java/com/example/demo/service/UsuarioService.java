package com.example.demo.service;

import com.example.demo.dto.PageResponseDTO;
import com.example.demo.dto.UsuarioCreateDTO;
import com.example.demo.dto.UsuarioResponseDTO;
import com.example.demo.dto.UsuarioUpdateDTO;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.ValidationException;
import com.example.demo.modelo.*;
import com.example.demo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.example.demo.repository.UsuarioSpecifications.*;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final SucursalRepository sucursalRepository;
    private final EspecialidadRepository especialidadRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // RN-CU01-02: tamaño de página por defecto (el wizard de "Listado de Usuarios"
    // permite elegir 10/25/50; si no envían "tamano" usamos 20 como default razonable)
    private static final int TAMANO_PAGINA_DEFAULT = 20;
    private static final int LONGITUD_MAX_BUSQUEDA = 25; // RN-CU01-01

    // Nombres EXACTOS de rol tal como están en la tabla `rol`
    private static final String ROL_ADMIN_SEDE = "Administrador";
    private static final String ROL_ADMIN_GENERAL = "Administrador General";

    // ------------------------------------------------------------------
    // Administrador de Sede vs Administrador General:
    // sucursalScope viene de AuthUsuario.sucursalScopeOrNull() (null si
    // quien llama es Administrador General -> sin restricción; un id si
    // es Administrador de Sede -> solo ve/edita usuarios de esa sucursal).
    // ------------------------------------------------------------------

    public List<UsuarioResponseDTO> listar(Integer sucursalScope) {
        Specification<Usuario> spec = Specification.where(sucursalIdEs(sucursalScope));
        return usuarioRepository.findAll(spec).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    // CU-01 (RN-CU01-01 / RN-CU01-02): "Listado de Usuarios" con filtro por campo y paginación
    public PageResponseDTO<UsuarioResponseDTO> buscar(String campo, String valor, Integer pagina, Integer tamano,
                                                        Integer sucursalScope) {
        int paginaSegura = (pagina == null || pagina < 0) ? 0 : pagina;
        int tamanoSeguro = (tamano == null || tamano <= 0) ? TAMANO_PAGINA_DEFAULT : tamano;

        if (valor != null && valor.length() > LONGITUD_MAX_BUSQUEDA) {
            throw new ValidationException("El campo de búsqueda no puede exceder los 25 caracteres.");
        }

        Pageable pageable = PageRequest.of(paginaSegura, tamanoSeguro, Sort.by("id").ascending());

        Specification<Usuario> spec = Specification.where(sucursalIdEs(sucursalScope));

        if (valor != null && !valor.isBlank()) {
            String texto = valor.trim();
            String campoNormalizado = campo == null ? "NOMBRE" : campo.trim().toUpperCase();

            Specification<Usuario> filtroCampo = switch (campoNormalizado) {
                case "ID" -> idComoEntero(texto);
                case "CORREO", "CORREO ELECTRONICO", "CORREO ELECTRÓNICO" -> correoContiene(texto);
                case "ROL" -> rolNombreContiene(texto);
                case "USUARIO", "NOMBRE DE USUARIO", "NOMBRE_USUARIO" -> nombreUsuarioContiene(texto);
                case "DPI" -> dpiContiene(texto);
                default -> nombreCompletoContiene(texto); // "NOMBRE"
            };
            spec = spec.and(filtroCampo);
        }
        // FA02 - Limpiar búsqueda: sin texto, solo queda el filtro de sede (o ninguno)

        Page<Usuario> resultado = usuarioRepository.findAll(spec, pageable);

        List<UsuarioResponseDTO> contenido = resultado.getContent().stream().map(this::toResponseDTO).toList();

        // FA03 - No se encontró información: contenido vacío, el frontend muestra el mensaje correspondiente
        return new PageResponseDTO<>(contenido, resultado.getNumber(), resultado.getSize(),
                resultado.getTotalElements(), resultado.getTotalPages());
    }

    // Filtro "ID" tolerante: si no es numérico, simplemente no matchea nada (en vez de reventar).
    private Specification<Usuario> idComoEntero(String texto) {
        try {
            Integer id = Integer.parseInt(texto);
            return idEs(id);
        } catch (NumberFormatException e) {
            return (root, query, cb) -> cb.disjunction(); // 0 resultados
        }
    }

    // NUEVO — para precargar el formulario de edición
    public UsuarioResponseDTO obtenerPorId(Integer id, Integer sucursalScope) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado."));
        validarDentroDeSede(usuario, sucursalScope);
        return toResponseDTO(usuario);
    }

    @Transactional
    public UsuarioResponseDTO crear(UsuarioCreateDTO dto, Integer sucursalScope) {
        if (usuarioRepository.existsByNombreUsuario(dto.getNombreUsuario())) {
            throw new ValidationException(
                    "El nombre de usuario " + dto.getNombreUsuario() + " ya se encuentra registrado. Por favor, elija otro.");
        }
        if (usuarioRepository.existsByCorreoElectronico(dto.getCorreoElectronico())) {
            throw new ValidationException("Ya existe una cuenta registrada con este correo electrónico.");
        }

        Rol rol = rolRepository.findById(dto.getRolId())
                .orElseThrow(() -> new ValidationException("Debe seleccionar un rol para el usuario."));

        // Un Administrador de Sede solo puede crear usuarios DENTRO de su propia
        // sede, y no puede crear otro Administrador General.
        if (sucursalScope != null) {
            if (ROL_ADMIN_GENERAL.equalsIgnoreCase(rol.getNombre())) {
                throw new ValidationException("Un Administrador de Sede no puede crear un Administrador General.");
            }
            if (dto.getSucursalId() != null && !dto.getSucursalId().equals(sucursalScope)) {
                throw new ValidationException("Solo puede crear usuarios de su propia sede.");
            }
        }

        Sucursal sucursal = resolverSucursal(rol, dto.getSucursalId(), sucursalScope);

        Especialidad especialidad = null;
        if (dto.getEspecialidadId() != null) {
            especialidad = especialidadRepository.findById(dto.getEspecialidadId())
                    .orElseThrow(() -> new ValidationException("Debe seleccionar una especialidad para el médico."));
        }

        Usuario usuario = new Usuario();
        usuario.setNombreCompleto(dto.getNombreCompleto());
        usuario.setCorreoElectronico(dto.getCorreoElectronico());
        usuario.setNombreUsuario(dto.getNombreUsuario());
        usuario.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        usuario.setDpi(dto.getDpi());
        usuario.setTelefono(dto.getTelefono());
        usuario.setNit(dto.getNit());
        usuario.setNumeroSeguro(dto.getNumeroSeguro());
        usuario.setRol(rol);
        usuario.setSucursal(sucursal);
        usuario.setEspecialidad(especialidad);
        usuario.setEstado(dto.getEstado());

        return toResponseDTO(usuarioRepository.save(usuario));
    }

    // NUEVO — CU-01 FA04 (Editar usuario)
    @Transactional
    public UsuarioResponseDTO actualizar(Integer id, UsuarioUpdateDTO dto, Integer sucursalScope) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado."));
        validarDentroDeSede(usuario, sucursalScope);

        if (usuarioRepository.existsByNombreUsuarioAndIdNot(dto.getNombreUsuario(), id)) {
            throw new ValidationException(
                    "El nombre de usuario " + dto.getNombreUsuario() + " ya se encuentra registrado. Por favor, elija otro.");
        }
        if (usuarioRepository.existsByCorreoElectronicoAndIdNot(dto.getCorreoElectronico(), id)) {
            throw new ValidationException("Ya existe una cuenta registrada con este correo electrónico.");
        }

        Rol rol = rolRepository.findById(dto.getRolId())
                .orElseThrow(() -> new ValidationException("Debe seleccionar un rol para el usuario."));

        if (sucursalScope != null) {
            if (ROL_ADMIN_GENERAL.equalsIgnoreCase(rol.getNombre())) {
                throw new ValidationException("Un Administrador de Sede no puede asignar el rol Administrador General.");
            }
            if (dto.getSucursalId() != null && !dto.getSucursalId().equals(sucursalScope)) {
                throw new ValidationException("Solo puede mover usuarios dentro de su propia sede.");
            }
        }

        // RN-CU01-13: sucursal es opcional en edición (a diferencia de creación),
        // salvo para roles que la requieren obligatoriamente (ver resolverSucursal).
        Sucursal sucursal = resolverSucursal(rol, dto.getSucursalId(), sucursalScope);

        Especialidad especialidad = null;
        if (dto.getEspecialidadId() != null) {
            especialidad = especialidadRepository.findById(dto.getEspecialidadId())
                    .orElseThrow(() -> new ValidationException("Debe seleccionar una especialidad para el médico."));
        }

        usuario.setNombreCompleto(dto.getNombreCompleto());
        usuario.setCorreoElectronico(dto.getCorreoElectronico());
        usuario.setNombreUsuario(dto.getNombreUsuario());
        // RN-CU01-06 (edición): la contraseña es opcional al editar.
        // Si viene vacía, no se toca. Si viene con contenido, debe
        // cumplir la misma regla de longitud mínima que en creación.
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            if (dto.getPassword().length() < 12) {
                throw new ValidationException("La contraseña debe contener al menos 12 caracteres.");
            }
            usuario.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        }
        usuario.setDpi(dto.getDpi());
        usuario.setTelefono(dto.getTelefono());
        usuario.setNit(dto.getNit());
        usuario.setNumeroSeguro(dto.getNumeroSeguro());
        usuario.setRol(rol);
        usuario.setSucursal(sucursal);
        usuario.setEspecialidad(especialidad);
        if (dto.getEstado() != null) {
            usuario.setEstado(dto.getEstado());
        }

        return toResponseDTO(usuarioRepository.save(usuario));
    }

    @Transactional
    public void eliminar(Integer id, Integer sucursalScope) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado."));
        validarDentroDeSede(usuario, sucursalScope);
        usuarioRepository.delete(usuario);
    }

    // Un Administrador de Sede (sucursalScope != null) no puede leer/editar/eliminar
    // usuarios de otra sede (incluido el Administrador General, que no tiene sede).
    private void validarDentroDeSede(Usuario usuario, Integer sucursalScope) {
        if (sucursalScope == null) return; // Administrador General: sin restricción
        Integer sucursalUsuario = usuario.getSucursal() != null ? usuario.getSucursal().getId() : null;
        if (!sucursalScope.equals(sucursalUsuario)) {
            throw new ResourceNotFoundException("Usuario no encontrado."); // no revelamos que existe en otra sede
        }
    }

    // Centraliza la regla de "¿esta sucursal es obligatoria, opcional o debe ser null
    // según el rol elegido?", tanto para creación como edición.
    private Sucursal resolverSucursal(Rol rol, Integer sucursalIdSolicitada, Integer sucursalScope) {
        boolean requiereSucursal = !ROL_ADMIN_GENERAL.equalsIgnoreCase(rol.getNombre())
                && !"Paciente".equalsIgnoreCase(rol.getNombre());

        // Un Administrador de Sede que gestiona usuarios de su propia sede: si no
        // manda sucursalId explícito, se asume la suya.
        Integer sucursalIdEfectiva = sucursalIdSolicitada != null ? sucursalIdSolicitada : sucursalScope;

        if (ROL_ADMIN_GENERAL.equalsIgnoreCase(rol.getNombre()) && sucursalIdEfectiva != null) {
            throw new ValidationException("Un Administrador General no debe estar asignado a una sola sede.");
        }

        if (!requiereSucursal) {
            return null; // Administrador General o Paciente: sin sede
        }

        if (sucursalIdEfectiva == null) {
            throw new ValidationException("Debe seleccionar una sucursal para el usuario.");
        }
        return sucursalRepository.findById(sucursalIdEfectiva)
                .orElseThrow(() -> new ValidationException("Debe seleccionar una sucursal para el usuario."));
    }

    private UsuarioResponseDTO toResponseDTO(Usuario u) {
        return new UsuarioResponseDTO(
                u.getId(), u.getNombreCompleto(), u.getCorreoElectronico(),
                u.getNombreUsuario(), u.getDpi(), u.getTelefono(),
                u.getRol().getNombre(),
                u.getSucursal() != null ? u.getSucursal().getId() : null,
                u.getSucursal() != null ? u.getSucursal().getNombre() : null,
                u.getEspecialidad() != null ? u.getEspecialidad().getNombre() : null,
                u.getEstado());
    }
}