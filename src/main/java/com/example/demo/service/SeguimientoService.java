package com.example.demo.service;

import com.example.demo.dto.SeguimientoDTOs.*;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.ValidationException;
import com.example.demo.modelo.*;
import com.example.demo.repository.CitaRepository;
import com.example.demo.repository.CitaSeguimientoRepository;
import com.example.demo.repository.ConsultaMedicaRepository;
import com.example.demo.repository.EstadoCitaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SeguimientoService {
    private final AuditoriaContexto auditoriaContexto;

    private final ConsultaMedicaRepository consultaMedicaRepository;
    private final CitaRepository citaRepository;
    private final EstadoCitaRepository estadoCitaRepository;
    private final CitaSeguimientoRepository citaSeguimientoRepository;
    private final CitaService citaService; // reutiliza el cálculo de horarios disponibles de CU-03
    private final EmailService emailService;

    @Value("${app.consulta.precio:150.00}")
    private BigDecimal precioConsulta;

    private static final Set<String> TIPOS_VALIDOS = Set.of(
            CitaSeguimiento.MONITOREO_TRATAMIENTO, CitaSeguimiento.REVISION_RESULTADOS);

    // ---------------------------------------------------------------
    // Paso 2-3 FB: banner con los datos precargados de la consulta padre
    // ---------------------------------------------------------------
    @Transactional(readOnly = true)
    public ContextoSeguimientoDTO obtenerContexto(Integer idConsulta, Integer medicoId) {
        ConsultaMedica consulta = obtenerConsultaDelMedico(idConsulta, medicoId);
        Cita citaOrigen = consulta.getCita();

        return ContextoSeguimientoDTO.builder()
                .consultaId(consulta.getId())
                .citaOrigenId(citaOrigen.getId())
                .pacienteId(citaOrigen.getPaciente().getId())
                .nombrePaciente(citaOrigen.getPaciente().getNombreCompleto())
                .medicoId(consulta.getMedico().getId())
                .nombreMedico(consulta.getMedico().getNombreCompleto())
                .sucursalId(citaOrigen.getSucursal().getId())
                .nombreSucursal(citaOrigen.getSucursal().getNombre())
                .especialidadId(citaOrigen.getEspecialidad().getId())
                .nombreEspecialidad(citaOrigen.getEspecialidad().getNombre())
                .build();
    }

    // ---------------------------------------------------------------
    // Pasos 4-8 FB: confirmar el agendamiento (RN-CU11-01/02/03, FA01)
    // ---------------------------------------------------------------
    @Transactional
    public SeguimientoResponseDTO crearSeguimiento(Integer idConsulta, Integer medicoId, CrearSeguimientoRequestDTO req) {
        auditoriaContexto.aplicar();
        ConsultaMedica consulta = obtenerConsultaDelMedico(idConsulta, medicoId);
        Cita citaOrigen = consulta.getCita();
        Usuario medico = consulta.getMedico();

        // RN-CU11-01
        String tipo = req.getTipoSeguimiento() == null ? "" : req.getTipoSeguimiento().trim().toUpperCase();
        if (!TIPOS_VALIDOS.contains(tipo)) {
            throw new ValidationException("Debe seleccionar el tipo de seguimiento.");
        }

        // RN-CU11-03 ("Observaciones")
        String motivo = req.getMotivoSeguimiento();
        if (motivo == null || motivo.length() < 10 || motivo.length() > 2000) {
            throw new ValidationException(
                    "Las observaciones son obligatorias. Deben contener entre 10 y 2000 caracteres.");
        }

        // RN-CU11-02: fecha futura...
        if (req.getFechaHora() == null || !req.getFechaHora().isAfter(LocalDateTime.now())) {
            throw new ValidationException(
                    "Seleccione una fecha futura dentro de los horarios disponibles del médico.");
        }

        // ...y que coincida con un horario realmente disponible del médico (mismo cálculo que CU-03)
        List<LocalDateTime> disponibles =
                citaService.listarHorariosDisponibles(medicoId, req.getFechaHora().toLocalDate());
        if (!disponibles.contains(req.getFechaHora())) {
            // FA01 - conflicto de horario
            throw new ValidationException(
                    "El horario seleccionado ya no está disponible. Por favor, elija otro horario.");
        }

        // La cita de seguimiento nace "Confirmada": es continuidad de la atención
        // ya iniciada por el médico, no pasa por el flujo de pago (CU-04/CU-06).
        EstadoCita confirmada = estadoCitaRepository.findByNombre(EstadoCita.CONFIRMADA)
                .orElseThrow(() -> new ResourceNotFoundException("Catálogo estado_cita incompleto: falta 'Confirmada'."));

        Cita citaSeguimiento = new Cita();
        citaSeguimiento.setPaciente(citaOrigen.getPaciente());
        citaSeguimiento.setMedico(medico);
        citaSeguimiento.setSucursal(citaOrigen.getSucursal());
        citaSeguimiento.setEspecialidad(citaOrigen.getEspecialidad());
        citaSeguimiento.setEstadoCita(confirmada);
        citaSeguimiento.setFechaHora(req.getFechaHora());
        citaSeguimiento.setMotivoConsulta(motivo);
        citaSeguimiento.setMonto(precioConsulta);
        Cita citaGuardada = citaRepository.save(citaSeguimiento);

        CitaSeguimiento seguimiento = new CitaSeguimiento();
        seguimiento.setConsultaOrigen(consulta);
        seguimiento.setCitaNueva(citaGuardada);
        seguimiento.setTipoSeguimiento(tipo);
        seguimiento.setMotivoSeguimiento(motivo);
        CitaSeguimiento seguimientoGuardado = citaSeguimientoRepository.save(seguimiento);

        // RN-CU11-04 (responsabilidad del backend)
        emailService.enviarSeguimientoAgendado(citaGuardada, seguimientoGuardado);

        String etiquetaTipo = CitaSeguimiento.MONITOREO_TRATAMIENTO.equals(tipo)
                ? "Monitoreo de Tratamiento" : "Revisión de Resultados de Laboratorio";
        String mensaje = String.format(
                "Cita de seguimiento agendada exitosamente. Tipo: %s. Paciente: %s.",
                etiquetaTipo, citaOrigen.getPaciente().getNombreCompleto());

        return SeguimientoResponseDTO.builder()
                .mensaje(mensaje)
                .citaSeguimientoId(seguimientoGuardado.getId())
                .citaNuevaId(citaGuardada.getId())
                .tipoSeguimiento(tipo)
                .fechaHora(citaGuardada.getFechaHora())
                .build();
    }

    // ---------------------------------------------------------------
    // RN-CU11-05 / RNF-020: recordatorio 1-2 días antes, resiliente a
    // reinicios (se marca recordatorio_enviado en BD, no en memoria).
    // No se envía si la cita fue cancelada.
    // ---------------------------------------------------------------
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void enviarRecordatoriosSeguimiento() {
        auditoriaContexto.aplicar();
        LocalDateTime desde = LocalDateTime.now().plusDays(1).toLocalDate().atStartOfDay();
        LocalDateTime hasta = LocalDateTime.now().plusDays(2).toLocalDate().atStartOfDay();

        List<CitaSeguimiento> pendientes = citaSeguimientoRepository
                .findByRecordatorioEnviadoFalseAndCitaNueva_FechaHoraBetween(desde, hasta);

        for (CitaSeguimiento cs : pendientes) {
            if (EstadoCita.CANCELADA.equals(cs.getCitaNueva().getEstadoCita().getNombre())) {
                continue;
            }
            emailService.enviarRecordatorioSeguimiento(cs.getCitaNueva());
            cs.setRecordatorioEnviado(true);
            citaSeguimientoRepository.save(cs);
        }
    }

    // ---------------------------------------------------------------
    private ConsultaMedica obtenerConsultaDelMedico(Integer idConsulta, Integer medicoId) {
        ConsultaMedica consulta = consultaMedicaRepository.findById(idConsulta)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la consulta indicada."));
        if (!consulta.getMedico().getId().equals(medicoId)) {
            throw new ResourceNotFoundException("No se encontró la consulta indicada para este médico.");
        }
        return consulta;
    }
}