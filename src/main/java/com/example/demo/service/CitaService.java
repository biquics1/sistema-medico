package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.exception.ValidationException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.modelo.*;
import com.example.demo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CitaService {

    private final CitaRepository citaRepository;
    private final SucursalRepository sucursalRepository;
    private final EspecialidadRepository especialidadRepository;
    private final BranchSpecialtyRepository branchSpecialtyRepository;
    private final UsuarioRepository usuarioRepository;
    private final EstadoCitaRepository estadoCitaRepository;
    private final AuditoriaContexto auditoriaContexto;

    @Value("${app.consulta.precio:150.00}")
    private BigDecimal precioConsulta;

    @Value("${app.reserva.online.minutos:5}")
    private long minutosReservaOnline;

    private static final LocalTime INICIO_JORNADA = LocalTime.of(8, 0);
    private static final LocalTime FIN_JORNADA = LocalTime.of(17, 0);
    private static final int DURACION_SLOT_MIN = 30;

    private static final List<String> ESTADOS_LIBERAN_HORARIO =
            List.of(EstadoCita.CANCELADA, EstadoCita.NO_ASISTIO);

    private static final List<String> ESTADOS_LIBERAN_ESPECIALIDAD =
            List.of(EstadoCita.CANCELADA, EstadoCita.NO_ASISTIO, EstadoCita.ATENCION_FINALIZADA);

    public List<Sucursal> listarSucursalesActivas() {
        return sucursalRepository.findByEstado((short) 1);
    }

    public List<Especialidad> listarEspecialidadesPorSucursal(Integer sucursalId) {
        List<BranchSpecialty> asignaciones = branchSpecialtyRepository
                .findBySucursal_IdAndEstado(sucursalId, (short) 1);
        if (asignaciones.isEmpty()) {
            throw new ValidationException(
                    "No hay especialidades disponibles para la sucursal seleccionada. Seleccione otra sucursal.");
        }
        return asignaciones.stream().map(BranchSpecialty::getEspecialidad).collect(Collectors.toList());
    }

    public List<DoctorDisponibleDTO> listarMedicos(Integer sucursalId, Integer especialidadId) {
        List<Usuario> medicos = usuarioRepository
                .findByRol_NombreAndSucursal_IdAndEspecialidad_IdAndEstado(
                        "Médico", sucursalId, especialidadId, (short) 1);
        if (medicos.isEmpty()) {
            throw new ValidationException(
                    "No se encontraron horarios disponibles para la especialidad en la sede seleccionada. " +
                            "Por favor, seleccione otra especialidad o sede.");
        }
        return medicos.stream()
                .map(m -> new DoctorDisponibleDTO(m.getId(), m.getNombreCompleto()))
                .collect(Collectors.toList());
    }

    public List<LocalDateTime> listarHorariosDisponibles(Integer medicoId, LocalDate fecha) {
        if (fecha.getDayOfWeek() == DayOfWeek.SATURDAY || fecha.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return List.of();
        }

        LocalDateTime desde = fecha.atTime(INICIO_JORNADA);
        LocalDateTime hasta = fecha.atTime(FIN_JORNADA);

        List<Cita> ocupadas = citaRepository.findByMedico_IdAndFechaHoraBetweenAndEstadoCita_NombreNotIn(
                medicoId, desde, hasta, ESTADOS_LIBERAN_HORARIO);

        Set<LocalDateTime> ocupadasSet = ocupadas.stream()
                .map(Cita::getFechaHora).collect(Collectors.toSet());

        List<LocalDateTime> disponibles = new ArrayList<>();
        LocalDateTime cursor = desde;
        while (cursor.isBefore(hasta)) {
            if (!ocupadasSet.contains(cursor) && cursor.isAfter(LocalDateTime.now())) {
                disponibles.add(cursor);
            }
            cursor = cursor.plusMinutes(DURACION_SLOT_MIN);
        }
        return disponibles;
    }

    @Transactional
    public CitaResponseDTO crearCita(CitaCreateDTO dto) {
        auditoriaContexto.aplicar();
        if (dto.getMotivoConsulta() == null ||
                dto.getMotivoConsulta().length() < 10 || dto.getMotivoConsulta().length() > 2000) {
            throw new ValidationException(
                    "El motivo debe contener entre 10 y 2000 caracteres.");
        }
        if (dto.getFechaHora() == null || !dto.getFechaHora().isAfter(LocalDateTime.now())) {
            throw new ValidationException(
                    "Debe seleccionar una fecha y hora futuras. Las citas no pueden agendarse en fechas pasadas o presentes.");
        }

        Usuario paciente = usuarioRepository.findById(dto.getPacienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado."));
        Usuario medico = usuarioRepository.findById(dto.getMedicoId())
                .orElseThrow(() -> new ResourceNotFoundException("Médico no encontrado."));
        Sucursal sucursal = sucursalRepository.findById(dto.getSucursalId())
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada."));
        Especialidad especialidad = especialidadRepository.findById(dto.getEspecialidadId())
                .orElseThrow(() -> new ResourceNotFoundException("Especialidad no encontrada."));

        boolean tieneCitaActivaEnEspecialidad = citaRepository
                .existsByPaciente_IdAndEspecialidad_IdAndEstadoCita_NombreNotIn(
                        paciente.getId(), especialidad.getId(), ESTADOS_LIBERAN_ESPECIALIDAD);
        if (tieneCitaActivaEnEspecialidad) {
            throw new ValidationException(
                    "Ya tiene una cita activa para la especialidad '" + especialidad.getNombre() +
                            "'. Debe esperar a que finalice o se cancele antes de agendar otra de la misma especialidad.");
        }

        boolean horarioOcupado = citaRepository.existsByMedico_IdAndFechaHoraAndEstadoCita_NombreNotIn(
                medico.getId(), dto.getFechaHora(), ESTADOS_LIBERAN_HORARIO);
        if (horarioOcupado) {
            throw new ValidationException(
                    "El horario seleccionado ya no está disponible. Por favor, elija otro horario.");
        }

        EstadoCita pendiente = estadoCitaRepository.findByNombre("Pendiente de pago")
                .orElseThrow(() -> new ResourceNotFoundException("Estado 'Pendiente de pago' no configurado."));

        Cita cita = new Cita();
        cita.setPaciente(paciente);
        cita.setMedico(medico);
        cita.setSucursal(sucursal);
        cita.setEspecialidad(especialidad);
        cita.setEstadoCita(pendiente);
        cita.setFechaHora(dto.getFechaHora());
        cita.setMotivoConsulta(dto.getMotivoConsulta());
        cita.setMonto(precioConsulta);
        // Se mantiene "Pendiente de pago" hasta el final del día de la CITA (no de la creación),
        // así el paciente puede agendar con una semana/mes de anticipación sin que se cancele antes de tiempo.
        cita.setExpiraEn(dto.getFechaHora().toLocalDate().atTime(LocalTime.MAX));

        try {
            cita = citaRepository.save(cita);
        } catch (DataIntegrityViolationException e) {
            throw new ValidationException(
                    "El horario o la especialidad seleccionados ya no están disponibles. Por favor, intente nuevamente.");
        }
        return toDto(cita);
    }

    public CitaResponseDTO obtenerCita(Integer id) {
        Cita cita = citaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada."));
        return toDto(cita);
    }

    @Transactional
    public CitaResponseDTO elegirMetodoPago(Integer citaId, Integer pacienteId, String metodoPago) {
        auditoriaContexto.aplicar();
        Cita cita = citaRepository.findByIdAndPaciente_Id(citaId, pacienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada."));

        if (!EstadoCita.PENDIENTE_PAGO.equals(cita.getEstadoCita().getNombre())) {
            throw new ValidationException("Esta cita ya no está pendiente de pago.");
        }
        if (cita.getExpiraEn() != null && cita.getExpiraEn().isBefore(LocalDateTime.now())) {
            throw new ValidationException(
                    "El tiempo para confirmar su cita ha expirado. El horario seleccionado ha sido liberado. " +
                            "Por favor, seleccione un nuevo horario.");
        }

        String metodo = metodoPago == null ? "" : metodoPago.trim().toUpperCase();
        switch (metodo) {
            // "expiraEn" NO se toca aquí: se mantiene como fin del día de la cita en ambos casos.
            case "LINEA" -> cita.setSesionPagoExpiraEn(
                    LocalDateTime.now().plusMinutes(minutosReservaOnline).withNano(0));
            case "CAJA" -> cita.setSesionPagoExpiraEn(null);
            default -> throw new ValidationException(
                    "Debe seleccionar un método de pago válido: 'CAJA' o 'LINEA'.");
        }

        cita = citaRepository.save(cita);
        return toDto(cita);
    }

    // El contador de 5 min de pago en línea (sesionPagoExpiraEn) llega a 0 sin
    // completarse el pago: se cancela la cita de inmediato (CU-04 FA02).
    // Esto es independiente del job de medianoche (expiraEn), que sigue
    // existiendo como respaldo general para citas pagadas en CAJA.
    @Transactional
    public CitaResponseDTO cancelarPorExpiracionInmediata(Integer citaId, Integer pacienteId) {
        auditoriaContexto.aplicar();
        Cita cita = citaRepository.findByIdAndPaciente_Id(citaId, pacienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada."));

        boolean pendiente = EstadoCita.PENDIENTE_PAGO.equals(cita.getEstadoCita().getNombre());
        boolean sesionVencida = cita.getSesionPagoExpiraEn() != null
                && !cita.getSesionPagoExpiraEn().isAfter(LocalDateTime.now());

        if (pendiente && sesionVencida) {
            EstadoCita cancelada = estadoCitaRepository.findByNombre(EstadoCita.CANCELADA)
                    .orElseThrow(() -> new ResourceNotFoundException("Estado 'Cancelada' no configurado."));
            cita.setEstadoCita(cancelada);
            cita = citaRepository.save(cita);
        }
        return toDto(cita);
    }

    public List<CitaAgendaDTO> listarCitasAgenda(Integer medicoId, LocalDateTime desde, LocalDateTime hasta) {
        return citaRepository.findParaAgendaMedico(medicoId, desde, hasta).stream()
                .map(c -> new CitaAgendaDTO(
                        c.getId(),
                        c.getPaciente().getNombreCompleto(),
                        c.getEspecialidad().getNombre(),
                        c.getEstadoCita().getNombre(),
                        c.getFechaHora(),
                        c.isEsEmergencia(),
                        c.getMotivoConsulta()
                ))
                .collect(Collectors.toList());
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void cancelarCitasExpiradas() {
        List<Cita> expiradas = citaRepository
                .findByEstadoCita_NombreAndExpiraEnBefore("Pendiente de pago", LocalDateTime.now());
        if (expiradas.isEmpty()) return;

        EstadoCita cancelada = estadoCitaRepository.findByNombre("Cancelada")
                .orElseThrow(() -> new ResourceNotFoundException("Estado 'Cancelada' no configurado."));
        for (Cita c : expiradas) {
            c.setEstadoCita(cancelada);
            citaRepository.save(c);
        }
    }

    private CitaResponseDTO toDto(Cita c) {
        return new CitaResponseDTO(
                c.getId(),
                c.getPaciente().getNombreCompleto(),
                c.getMedico().getNombreCompleto(),
                c.getSucursal().getNombre(),
                c.getEspecialidad().getNombre(),
                c.getEstadoCita().getNombre(),
                c.getFechaHora(),
                c.getMotivoConsulta(),
                c.getMonto(),
                c.getExpiraEn(),
                c.getSesionPagoExpiraEn()
        );
    }
}