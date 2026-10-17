package com.example.demo.service;

import com.example.demo.dto.SeguimientoDTOs.*;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.ValidationException;
import com.example.demo.modelo.*;
import com.example.demo.repository.CitaRepository;
import com.example.demo.repository.CitaSeguimientoRepository;
import com.example.demo.repository.ConsultaMedicaRepository;
import com.example.demo.repository.EstadoCitaRepository;
import com.example.demo.security.AuthUsuario;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
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
    private final EmailService emailService;

    @Value("${app.consulta.precio:150.00}")
    private BigDecimal precioConsulta;

    private static final Set<String> TIPOS_VALIDOS = Set.of(
            CitaSeguimiento.MONITOREO_TRATAMIENTO, CitaSeguimiento.REVISION_RESULTADOS);
    private static final List<String> ESTADOS_LIBERAN_HORARIO = List.of(
            EstadoCita.CANCELADA, EstadoCita.NO_ASISTIO);

    @Transactional(readOnly = true)
    public ContextoSeguimientoDTO obtenerContexto(Integer idConsulta, AuthUsuario usuario) {
        ConsultaMedica consulta = obtenerConsultaAutorizada(idConsulta, usuario);
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

    @Transactional
    public SeguimientoResponseDTO crearSeguimiento(Integer idConsulta, AuthUsuario usuario,
                                                   CrearSeguimientoRequestDTO req) {
        auditoriaContexto.aplicar();
        ConsultaMedica consulta = obtenerConsultaAutorizada(idConsulta, usuario);
        Cita citaOrigen = consulta.getCita();
        Usuario medico = consulta.getMedico();
        Integer medicoId = medico.getId(); // la agenda que se valida es la del médico de la consulta

        // 1. Validar Tipo de Seguimiento
        String tipo = req.getTipoSeguimiento() == null ? "" : req.getTipoSeguimiento().trim().toUpperCase();
        if (!TIPOS_VALIDOS.contains(tipo)) {
            throw new ValidationException("Debe seleccionar el tipo de seguimiento.");
        }

        // 2. Validar Observaciones
        String motivo = req.getMotivoSeguimiento();
        if (motivo == null || motivo.length() < 10 || motivo.length() > 2000) {
            throw new ValidationException("Las observaciones son obligatorias. Deben contener entre 10 y 2000 caracteres.");
        }

        // 3. Normalización y validación de fecha/hora
        if (req.getFechaHora() == null) {
            throw new ValidationException("Debe seleccionar una fecha y hora válidas.");
        }
        LocalDateTime fechaHoraNormalizada = req.getFechaHora().withSecond(0).withNano(0);
        if (!fechaHoraNormalizada.isAfter(LocalDateTime.now())) {
            throw new ValidationException("Seleccione una fecha futura dentro de los horarios disponibles del médico.");
        }
        DayOfWeek dia = fechaHoraNormalizada.getDayOfWeek();
        if (dia == DayOfWeek.SATURDAY || dia == DayOfWeek.SUNDAY) {
            throw new ValidationException("Las citas de seguimiento solo pueden agendarse de lunes a viernes.");
        }
        LocalTime hora = fechaHoraNormalizada.toLocalTime();
        if (hora.isBefore(LocalTime.of(8, 0)) || hora.isAfter(LocalTime.of(17, 0)) || hora.equals(LocalTime.of(17, 0))) {
            throw new ValidationException("El horario permitido es de 08:00 AM a 05:00 PM.");
        }

        // 4. Verificación directa de disponibilidad descartando citas canceladas/no asistió
        List<Cita> citasOcupadas = citaRepository.findByMedico_IdAndFechaHoraBetweenAndEstadoCita_NombreNotIn(
                medicoId,
                fechaHoraNormalizada.minusMinutes(1),
                fechaHoraNormalizada.plusMinutes(1),
                ESTADOS_LIBERAN_HORARIO
        );
        if (!citasOcupadas.isEmpty()) {
            throw new ValidationException("El médico ya tiene una cita agendada en la fecha y hora seleccionadas ("
                    + fechaHoraNormalizada.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                    + "). Por favor elija otro horario.");
        }

        // Cita de seguimiento nace como "Pendiente de pago": el paciente debe pagarla (caja o en línea)
        EstadoCita pendientePago = estadoCitaRepository.findByNombre(EstadoCita.PENDIENTE_PAGO)
                .orElseThrow(() -> new ResourceNotFoundException("Catálogo estado_cita incompleto: falta 'Pendiente de pago'."));

        Cita citaSeguimiento = new Cita();
        citaSeguimiento.setPaciente(citaOrigen.getPaciente());
        citaSeguimiento.setMedico(medico);
        citaSeguimiento.setSucursal(citaOrigen.getSucursal());
        citaSeguimiento.setEspecialidad(citaOrigen.getEspecialidad());
        citaSeguimiento.setEstadoCita(pendientePago);

        // Igual que en CitaService: si no se paga, se cancela al terminar el día de la cita
        citaSeguimiento.setExpiraEn(fechaHoraNormalizada.toLocalDate().atTime(LocalTime.MAX));
        citaSeguimiento.setFechaHora(fechaHoraNormalizada);
        citaSeguimiento.setMotivoConsulta(motivo);
        citaSeguimiento.setMonto(precioConsulta);

        // Marca la cita como seguimiento: el índice único (paciente + especialidad activa) la excluye,
        // así el médico puede agendarla aunque la cita origen aún no esté finalizada.
        citaSeguimiento.setEsSeguimiento(true);

        Cita citaGuardada;
        try {
            citaGuardada = citaRepository.save(citaSeguimiento);
        } catch (DataIntegrityViolationException e) {
            throw new ValidationException("El horario seleccionado ya no está disponible. Por favor, elija otro horario.");
        }

        CitaSeguimiento seguimiento = new CitaSeguimiento();
        seguimiento.setConsultaOrigen(consulta);
        seguimiento.setCitaNueva(citaGuardada);
        seguimiento.setTipoSeguimiento(tipo);
        seguimiento.setMotivoSeguimiento(motivo);

        CitaSeguimiento seguimientoGuardado = citaSeguimientoRepository.save(seguimiento);
        emailService.enviarSeguimientoAgendado(citaGuardada, seguimientoGuardado);

        String etiquetaTipo = CitaSeguimiento.MONITOREO_TRATAMIENTO.equals(tipo)
                ? "Monitoreo de Tratamiento" : "Revisión de Resultados de Laboratorio";

        String mensaje = String.format(
                "Cita de seguimiento agendada exitosamente para el %s. Tipo: %s. Paciente: %s. Estado: Pendiente de pago.",
                citaGuardada.getFechaHora().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                etiquetaTipo, citaOrigen.getPaciente().getNombreCompleto());

        return SeguimientoResponseDTO.builder()
                .mensaje(mensaje)
                .citaSeguimientoId(seguimientoGuardado.getId())
                .citaNuevaId(citaGuardada.getId())
                .tipoSeguimiento(tipo)
                .fechaHora(citaGuardada.getFechaHora())
                .build();
    }

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

    // Médico: solo sus consultas. Administrador General: cualquiera.
    // Administrador de Sede: solo consultas de citas de su propia sede.
    private ConsultaMedica obtenerConsultaAutorizada(Integer idConsulta, AuthUsuario usuario) {
        ConsultaMedica consulta = consultaMedicaRepository.findById(idConsulta)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la consulta indicada."));

        if (usuario.esAdminGeneral()) {
            return consulta;
        }

        if (usuario.esAdminSede()) {
            Integer sedeCita = consulta.getCita().getSucursal().getId();
            if (usuario.getSucursalId() == null || !usuario.getSucursalId().equals(sedeCita)) {
                throw new ResourceNotFoundException("No se encontró la consulta indicada para su sede.");
            }
            return consulta;
        }

        if (!consulta.getMedico().getId().equals(usuario.getId())) {
            throw new ResourceNotFoundException("No se encontró la consulta indicada para este médico.");
        }

        return consulta;
    }
}