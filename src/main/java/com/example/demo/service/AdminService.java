package com.example.demo.service;

import com.example.demo.dto.AdminMedicoDTO;
import com.example.demo.dto.CitaAgendaDTO;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.modelo.Usuario;
import com.example.demo.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Panel de "Control de Citas por Sede" para el Administrador.
 *
 * Administrador de Sede (sucursalScope != null): solo ve los médicos de su
 * propia sucursal y, para cada uno, su calendario/control de citas.
 * Administrador General (sucursalScope == null): ve médicos de todas las
 * sedes, sin restricción.
 */
@Service
@RequiredArgsConstructor
public class AdminService {

    private static final String ROL_MEDICO = "Médico";

    private final UsuarioRepository usuarioRepository;
    private final CitaService citaService;


    public List<AdminMedicoDTO> listarMedicos(Integer sucursalScope) {
        List<Usuario> medicos = (sucursalScope == null)
                ? usuarioRepository.findByRol_NombreAndEstadoOrderByNombreCompletoAsc(ROL_MEDICO, (short) 1)
                : usuarioRepository.findByRol_NombreAndSucursal_IdAndEstadoOrderByNombreCompletoAsc(
                        ROL_MEDICO, sucursalScope, (short) 1);

        return medicos.stream().map(this::toAdminMedicoDTO).toList();
    }

    // Agenda/control de citas de un médico puntual. Si quien consulta es
    // Administrador de Sede, se valida que el médico pertenezca a su sede
    // antes de mostrar nada (evita que "adivine" un id de otra sucursal).
    public List<CitaAgendaDTO> agendaDeMedico(Integer medicoId, LocalDateTime desde, LocalDateTime hasta,
                                               Integer sucursalScope) {
        Usuario medico = usuarioRepository.findById(medicoId)
                .filter(u -> ROL_MEDICO.equalsIgnoreCase(u.getRol().getNombre()))
                .orElseThrow(() -> new ResourceNotFoundException("Médico no encontrado."));

        if (sucursalScope != null) {
            Integer sucursalMedico = medico.getSucursal() != null ? medico.getSucursal().getId() : null;
            if (!sucursalScope.equals(sucursalMedico)) {
                // No revelamos que el médico existe en otra sede.
                throw new ResourceNotFoundException("Médico no encontrado.");
            }
        }

        return citaService.listarCitasAgenda(medicoId, desde, hasta);
    }

    private AdminMedicoDTO toAdminMedicoDTO(Usuario u) {
        return new AdminMedicoDTO(
                u.getId(),
                u.getNombreCompleto(),
                u.getCorreoElectronico(),
                u.getEspecialidad() != null ? u.getEspecialidad().getNombre() : null,
                u.getSucursal() != null ? u.getSucursal().getId() : null,
                u.getSucursal() != null ? u.getSucursal().getNombre() : null,
                u.getEstado()
        );
    }
}
