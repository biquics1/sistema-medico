package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.exception.ValidationException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.modelo.*;
import com.example.demo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${app.consulta.precio:150.00}")
    private BigDecimal precioConsulta;

    @Value("${app.reserva.minutos:5}")
    private long minutosReserva;

    // NUEVO: ventana de tiempo específica para el pago EN LÍNEA una vez que el
    // paciente elige ese método tras confirmar la cita (más corta que la
    // ventana genérica de arriba, que sigue aplicando mientras decide
    // "caja" vs "línea"). Default documentado: 5 minutos.
    @Value("${app.reserva.online.minutos:5}")
    private long minutosReservaOnline;

    private static final LocalTime INICIO_JORNADA = LocalTime.of(8, 0);
    private static final LocalTime FIN_JORNADA = LocalTime.of(17, 0);
    private static final int DURACION_SLOT_MIN = 30;

    // Paso 1: sucursales activas
    public List<Sucursal> listarSucursalesActivas() {
        return sucursalRepository.findByEstado((short) 1);
    }

    // Paso 2: especialidades por sucursal
    public List<Especialidad> listarEspecialidadesPorSucursal(Integer sucursalId) {
        List<BranchSpecialty> asignaciones = branchSpecialtyRepository
                .findBySucursal_IdAndEstado(sucursalId, (short) 1);
        if (asignaciones.isEmpty()) {
            throw new ValidationException(
                    "No hay especialidades disponibles para la sucursal seleccionada. Seleccione otra sucursal.");
        }
        return asignaciones.stream().map(BranchSpecialty::getEspecialidad).collect(Collectors.toList());
    }

    // Paso 3: médicos por sucursal + especialidad
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

    // Paso 4: horarios disponibles de un médico en una fecha
    public List<LocalDateTime> listarHorariosDisponibles(Integer medicoId, LocalDate fecha) {
        if (fecha.getDayOfWeek() == DayOfWeek.SATURDAY || fecha.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return List.of(); // sin atención en fin de semana (regla simplificada)
        }

        LocalDateTime desde = fecha.atTime(INICIO_JORNADA);
        LocalDateTime hasta = fecha.atTime(FIN_JORNADA);

        List<Cita> ocupadas = citaRepository.findByMedico_IdAndFechaHoraBetweenAndEstadoCita_NombreNot(
                medicoId, desde, hasta, "Cancelada");

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

    // Paso 5: crear cita
    @Transactional
    public CitaResponseDTO crearCita(CitaCreateDTO dto) {
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

        // Verificación de disponibilidad (evita doble reserva)
        boolean ocupado = citaRepository
                .findByMedico_IdAndFechaHoraBetweenAndEstadoCita_NombreNot(
                        medico.getId(),
                        dto.getFechaHora(), dto.getFechaHora().plusMinutes(1),
                        "Cancelada")
                .stream().anyMatch(c -> c.getFechaHora().equals(dto.getFechaHora()));
        if (ocupado) {
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
        cita.setExpiraEn(LocalDateTime.now().plusMinutes(minutosReserva));

        cita = citaRepository.save(cita);
        return toDto(cita);
    }

    public CitaResponseDTO obtenerCita(Integer id) {
        Cita cita = citaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada."));
        return toDto(cita);
    }

    // ------------------------------------------------------------------
    // NUEVO: paso "método de pago" tras confirmar la cita (wizard paso 6).
    // El paciente elige pagar en caja (sin vencimiento automático, igual
    // que el flujo actual) o en línea (se le da una ventana corta de
    // app.reserva.online.minutos, default 5, para completar el pago con
    // tarjeta antes de que la cita se cancele automáticamente).
    // ------------------------------------------------------------------
    @Transactional
    public CitaResponseDTO elegirMetodoPago(Integer citaId, Integer pacienteId, String metodoPago) {
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
            case "LINEA" -> cita.setExpiraEn(LocalDateTime.now().plusMinutes(minutosReservaOnline));
            case "CAJA" -> cita.setExpiraEn(null); // sin vencimiento automático: se paga físicamente en caja
            default -> throw new ValidationException(
                    "Debe seleccionar un método de pago válido: 'CAJA' o 'LINEA'.");
        }

        cita = citaRepository.save(cita);
        return toDto(cita);
    }

    // cancelación inmediata disparada por el propio cliente cuando el
    // contador regresivo de pago en línea llega a 0, para no tener que
    // esperar hasta 60s a que corra el job programado cancelarCitasExpiradas().
    // Es idempotente y segura: solo cancela si sigue "Pendiente de pago" Y
    // realmente ya venció; en cualquier otro caso simplemente devuelve el
    // estado actual de la cita sin tocar nada.
    @Transactional
    public CitaResponseDTO cancelarPorExpiracionInmediata(Integer citaId, Integer pacienteId) {
        Cita cita = citaRepository.findByIdAndPaciente_Id(citaId, pacienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada."));

        boolean pendiente = EstadoCita.PENDIENTE_PAGO.equals(cita.getEstadoCita().getNombre());
        boolean venciada = cita.getExpiraEn() != null && !cita.getExpiraEn().isAfter(LocalDateTime.now());

        if (pendiente && venciada) {
            EstadoCita cancelada = estadoCitaRepository.findByNombre(EstadoCita.CANCELADA)
                    .orElseThrow(() -> new ResourceNotFoundException("Estado 'Cancelada' no configurado."));
            cita.setEstadoCita(cancelada);
            cita = citaRepository.save(cita);
        }
        return toDto(cita);
    }

    // NUEVO — CU-14: citas del médico para pintarlas en su calendario de Agenda Médica.
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

    // Job: libera/cancela reservas vencidas sin pago (RN CU-03 FA03 / RNF-019)
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
                c.getExpiraEn()
        );
    }
}
