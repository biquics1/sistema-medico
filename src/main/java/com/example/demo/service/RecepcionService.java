package com.example.demo.service;

import com.example.demo.dto.CitaRecepcionDTO;
import com.example.demo.dto.RecepcionDTOs.*;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.ValidationException;
import com.example.demo.modelo.*;
import com.example.demo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class RecepcionService {

    private static final Pattern DPI_PATTERN = Pattern.compile("^\\d{13}$");

    private static final LocalTime INICIO_JORNADA = LocalTime.of(8, 0);
    private static final LocalTime FIN_JORNADA = LocalTime.of(17, 0);
    private static final List<String> ESTADOS_LIBERAN_HORARIO = List.of(EstadoCita.CANCELADA, EstadoCita.NO_ASISTIO);

    private final CitaRepository citaRepository;
    private final UsuarioRepository usuarioRepository;
    private final EstadoCitaRepository estadoCitaRepository;
    private final ReasignacionMedicoRepository reasignacionMedicoRepository;
    private final EmailService emailService;
    private final AuditoriaContexto auditoriaContexto;

    // ---------------------------------------------------------------
    // Paso 3 FB: búsqueda por número de cita o DPI (RN-CU05-01)
    // ---------------------------------------------------------------
    @Transactional(readOnly = true)
    public BusquedaResultadoDTO buscarCita(String tipo, String valor, Integer sucursalScope) {
        if (valor == null || valor.isBlank()) {
            throw new ValidationException("Debe ingresar un número de cita o DPI para buscar.");
        }
        return "CITA".equalsIgnoreCase(tipo) ? buscarPorNumeroCita(valor, sucursalScope) : buscarPorDpi(valor, sucursalScope);
    }

    private BusquedaResultadoDTO buscarPorNumeroCita(String valor, Integer sucursalScope) {
        Integer idCita;
        try {
            idCita = Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            throw new ResourceNotFoundException(
                    "No se encontró una cita asociada a los parámetros ingresados. Verifique los datos e intente nuevamente.");
        }

        Cita cita = citaRepository.findById(idCita)
                .filter(c -> perteneceASede(c, sucursalScope))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró una cita asociada a los parámetros ingresados. Verifique los datos e intente nuevamente."));

        return BusquedaResultadoDTO.builder()
                .citaEncontrada(true)
                .pacienteExiste(true)
                .cita(toDTO(cita))
                .mensaje(null)
                .accionSugerida(null)
                .build();
    }

    private boolean perteneceASede(Cita c, Integer sucursalScope) {
        return sucursalScope == null || sucursalScope.equals(c.getSucursal().getId());
    }

    // FA02 (Por DPI); también resuelve FA03 y FA04
    private BusquedaResultadoDTO buscarPorDpi(String dpi, Integer sucursalScope) {
        if (!DPI_PATTERN.matcher(dpi.trim()).matches()) {
            throw new ValidationException("El DPI debe contener exactamente 13 dígitos numéricos.");
        }

        List<String> estadosActivos = List.of(
                EstadoCita.PENDIENTE_PAGO,
                EstadoCita.CONFIRMADA,
                EstadoCita.PACIENTE_PRESENTE
        );

        List<Cita> citasActivas = citaRepository.buscarPorDpiPaciente(dpi.trim())
                .stream()
                .filter(c -> perteneceASede(c, sucursalScope))
                .filter(c -> estadosActivos.contains(c.getEstadoCita().getNombre()))
                .toList();

        if (!citasActivas.isEmpty()) {
            return BusquedaResultadoDTO.builder()
                    .citaEncontrada(true)
                    .pacienteExiste(true)
                    .cita(toDTO(elegirCitaMasRelevante(citasActivas)))
                    .mensaje(null)
                    .accionSugerida(null)
                    .build();
        }

        boolean pacienteExiste = usuarioRepository.findByDpi(dpi.trim()).isPresent();
        if (!pacienteExiste) {
            return BusquedaResultadoDTO.builder()
                    .citaEncontrada(false)
                    .pacienteExiste(false)
                    .cita(null)
                    .mensaje("No se encontró ningún paciente con ese DPI. Es necesario registrar al paciente antes de continuar.")
                    .accionSugerida("REGISTRAR_PACIENTE")
                    .build();
        }

        Usuario paciente = usuarioRepository.findByDpi(dpi.trim()).get();
        return BusquedaResultadoDTO.builder()
                .citaEncontrada(false)
                .pacienteExiste(true)
                .cita(null)
                .mensaje("El paciente " + paciente.getNombreCompleto() + " está registrado pero no tiene citas activas. Puede crear una nueva cita para este paciente.")
                .accionSugerida("NUEVA_CITA_WALKIN")
                .build();
    }

    private Cita elegirCitaMasRelevante(List<Cita> citasOrdenadas) {
        var hoy = java.time.LocalDate.now();
        return citasOrdenadas.stream()
                .filter(c -> c.getFechaHora().toLocalDate().equals(hoy))
                .findFirst()
                .orElse(citasOrdenadas.get(0));
    }

    // ---------------------------------------------------------------
    // Paso 6 FB: registrar llegada (FA05, FA06, FA08, FA09)
    // ---------------------------------------------------------------
    @Transactional
    public RegistrarLlegadaResponseDTO registrarLlegada(Integer idCita, Integer sucursalScope) {
        Cita cita = citaRepository.findById(idCita)
                .filter(c -> perteneceASede(c, sucursalScope))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró una cita asociada a los parámetros ingresados. Verifique los datos e intente nuevamente."));

        String estadoActual = cita.getEstadoCita().getNombre();

        if (EstadoCita.CANCELADA.equals(estadoActual)) {
            throw new ValidationException("La cita fue cancelada. El paciente debe agendar una nueva cita.");
        }
        if (EstadoCita.PENDIENTE_PAGO.equals(estadoActual)) {
            throw new ValidationException(
                    "La cita del paciente tiene estado 'Pendiente de pago'. Debe realizar el pago en caja antes de ser atendido.");
        }
        if (!EstadoCita.CONFIRMADA.equals(estadoActual)) {
            throw new ValidationException("La cita no se encuentra en un estado válido para registrar la llegada.");
        }

        EstadoCita pacientePresente = estadoCitaRepository.findByNombre(EstadoCita.PACIENTE_PRESENTE)
                .orElseThrow(() -> new ResourceNotFoundException("Catálogo estado_cita incompleto: falta 'Paciente Presente'."));

        try {
            cita.setEstadoCita(pacientePresente);
            cita.setHoraLlegada(LocalDateTime.now());
            Cita actualizada = citaRepository.save(cita);
            emailService.enviarLlegadaRegistrada(actualizada);

            String nombre = actualizada.getPaciente().getNombreCompleto();
            String mensaje = actualizada.isEsEmergencia()
                    ? "Paciente " + nombre + " registrado con prioridad de EMERGENCIA. "
                    + "El paciente debe pasar directamente a toma de signos vitales."
                    : "La llegada del paciente " + nombre + " ha sido registrada exitosamente. "
                    + "El paciente debe pasar a la sala de espera.";

            return RegistrarLlegadaResponseDTO.builder()
                    .mensaje(mensaje)
                    .cita(toDTO(actualizada))
                    .build();

        } catch (ObjectOptimisticLockingFailureException e) {
            throw new ValidationException("Operación no permitida");
        }
    }

    // ---------------------------------------------------------------
    // FA04 / CU-03: Agendar cita Walk-in desde Recepción
    // ---------------------------------------------------------------
    @Transactional
    public CitaRecepcionDTO agendarCitaWalkIn(AgendarWalkInRequestDTO request, Integer sucursalScope) {
        auditoriaContexto.aplicar();

        if (request.getDpiPaciente() == null || request.getDpiPaciente().isBlank()) {
            throw new ValidationException("El DPI del paciente es obligatorio.");
        }

        if (request.getFechaHora() == null || request.getFechaHora().isBlank()) {
            throw new ValidationException("Debe seleccionar la fecha y hora de la cita.");
        }

        LocalDateTime fechaHora = LocalDateTime.parse(request.getFechaHora());

        if (!fechaHora.isAfter(LocalDateTime.now())) {
            throw new ValidationException("Debe seleccionar una fecha y hora futuras.");
        }

        DayOfWeek dia = fechaHora.getDayOfWeek();
        if (dia == DayOfWeek.SATURDAY || dia == DayOfWeek.SUNDAY) {
            throw new ValidationException("Las citas solo pueden agendarse de lunes a viernes.");
        }

        LocalTime hora = fechaHora.toLocalTime();
        if (hora.isBefore(INICIO_JORNADA) || hora.isAfter(FIN_JORNADA) || hora.equals(FIN_JORNADA)) {
            throw new ValidationException("El horario de atención es de 08:00 AM a 05:00 PM.");
        }

        if (hora.getMinute() != 0 && hora.getMinute() != 30) {
            throw new ValidationException("Las citas deben agendarse en bloques de 30 minutos (ej. 08:00, 08:30).");
        }

        Usuario paciente = usuarioRepository.findByDpi(request.getDpiPaciente().trim())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró al paciente con el DPI proporcionado."));

        Usuario medico = usuarioRepository.findById(request.getMedicoId())
                .orElseThrow(() -> new ResourceNotFoundException("El médico seleccionado no existe."));

        boolean horarioOcupado = citaRepository.existsByMedico_IdAndFechaHoraAndEstadoCita_NombreNotIn(
                medico.getId(), fechaHora, ESTADOS_LIBERAN_HORARIO);
        if (horarioOcupado) {
            throw new ValidationException("El médico seleccionado ya tiene una cita agendada en ese horario. Por favor, seleccione otro horario.");
        }

        EstadoCita estadoPendiente = estadoCitaRepository.findByNombre("Pendiente de pago")
                .orElseThrow(() -> new ResourceNotFoundException("Estado 'Pendiente de pago' no configurado."));

        Cita nuevaCita = new Cita();
        nuevaCita.setPaciente(paciente);
        nuevaCita.setMedico(medico);
        nuevaCita.setEspecialidad(medico.getEspecialidad());
        nuevaCita.setSucursal(medico.getSucursal());
        nuevaCita.setEstadoCita(estadoPendiente);
        nuevaCita.setFechaHora(fechaHora);
        nuevaCita.setMotivoConsulta(request.getMotivoConsulta() != null && !request.getMotivoConsulta().isBlank()
                ? request.getMotivoConsulta()
                : "Consulta Walk-in presencial");
        nuevaCita.setMonto(new BigDecimal("150.00"));
        nuevaCita.setExpiraEn(fechaHora.toLocalDate().atTime(LocalTime.MAX));
        nuevaCita.setEsEmergencia(false);

        Cita guardada = citaRepository.save(nuevaCita);
        emailService.enviarCitaAgendada(guardada);

        return toDTO(guardada);
    }

    // ---------------------------------------------------------------
    // Cancelación de cita desde Recepción
    // ---------------------------------------------------------------
    @Transactional
    public CancelarCitaResponseDTO cancelarCita(Integer idCita, String motivo, Integer sucursalScope) {
        Cita cita = citaRepository.findById(idCita)
                .filter(c -> perteneceASede(c, sucursalScope))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró una cita asociada a los parámetros ingresados. Verifique los datos e intente nuevamente."));

        String estadoActual = cita.getEstadoCita().getNombre();

        if (EstadoCita.CANCELADA.equals(estadoActual)) {
            throw new ValidationException("La cita ya se encuentra cancelada.");
        }
        boolean cancelable = EstadoCita.PENDIENTE_PAGO.equals(estadoActual)
                || EstadoCita.CONFIRMADA.equals(estadoActual)
                || EstadoCita.PACIENTE_PRESENTE.equals(estadoActual);
        if (!cancelable) {
            throw new ValidationException(
                    "Solo se pueden cancelar citas en estado 'Pendiente de pago', 'Confirmada' o 'Paciente Presente'. "
                            + "Esta cita ya se encuentra en proceso de atención (estado actual: '" + estadoActual + "').");
        }

        EstadoCita cancelada = estadoCitaRepository.findByNombre(EstadoCita.CANCELADA)
                .orElseThrow(() -> new ResourceNotFoundException("Catálogo estado_cita incompleto: falta 'Cancelada'."));

        try {
            cita.setEstadoCita(cancelada);
            Cita actualizada = citaRepository.save(cita);
            emailService.enviarCitaCancelada(actualizada,
                    motivo != null && !motivo.isBlank() ? motivo : "Cancelada por recepción.");

            String nombre = actualizada.getPaciente().getNombreCompleto();
            String mensaje = "La cita #" + actualizada.getId() + " del paciente " + nombre
                    + " ha sido cancelada correctamente.";

            return CancelarCitaResponseDTO.builder()
                    .mensaje(mensaje)
                    .cita(toDTO(actualizada))
                    .build();
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new ValidationException("Operación no permitida");
        }
    }

    // ---------------------------------------------------------------
    // FA07: reasignación de médico
    // ---------------------------------------------------------------
    @Transactional
    public CitaRecepcionDTO reasignarMedico(Integer idCita, Integer idMedicoNuevo, String motivo,
                                            Integer idUsuarioReasigna, Integer sucursalScope) {
        Cita cita = citaRepository.findById(idCita)
                .filter(c -> perteneceASede(c, sucursalScope))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró una cita asociada a los parámetros ingresados. Verifique los datos e intente nuevamente."));

        String estadoActual = cita.getEstadoCita().getNombre();
        if (!EstadoCita.CONFIRMADA.equals(estadoActual) && !EstadoCita.PACIENTE_PRESENTE.equals(estadoActual)) {
            throw new ValidationException("Solo se puede reasignar médico en citas Confirmadas o con Paciente Presente.");
        }

        Usuario medicoNuevo = usuarioRepository.findById(idMedicoNuevo)
                .orElseThrow(() -> new ResourceNotFoundException("El médico seleccionado no existe."));
        Usuario medicoAnterior = cita.getMedico();

        if (medicoNuevo.getRol() == null || !"Médico".equalsIgnoreCase(medicoNuevo.getRol().getNombre())) {
            throw new ValidationException("El usuario seleccionado no es un médico.");
        }
        if (medicoNuevo.getEstado() == null || medicoNuevo.getEstado() != 1) {
            throw new ValidationException("El médico seleccionado no está activo.");
        }
        if (medicoNuevo.getEspecialidad() == null
                || !medicoNuevo.getEspecialidad().getId().equals(cita.getEspecialidad().getId())) {
            throw new ValidationException("El médico seleccionado no tiene la especialidad de la cita.");
        }
        if (medicoNuevo.getSucursal() == null
                || !medicoNuevo.getSucursal().getId().equals(cita.getSucursal().getId())) {
            throw new ValidationException("El médico seleccionado no pertenece a la sucursal de la cita.");
        }
        if (medicoNuevo.getId().equals(medicoAnterior.getId())) {
            throw new ValidationException("El médico seleccionado ya es el médico actual de la cita.");
        }

        Usuario usuarioReasigna = usuarioRepository.findById(idUsuarioReasigna)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario que reasigna no válido."));

        ReasignacionMedico registro = new ReasignacionMedico();
        registro.setCita(cita);
        registro.setMedicoAnterior(medicoAnterior);
        registro.setMedicoNuevo(medicoNuevo);
        registro.setUsuarioReasigna(usuarioReasigna);
        registro.setMotivo(motivo);
        reasignacionMedicoRepository.save(registro);

        cita.setMedico(medicoNuevo);
        Cita actualizada = citaRepository.save(cita);
        emailService.enviarMedicoReasignado(actualizada, medicoAnterior);

        return toDTO(actualizada);
    }

    // ---------------------------------------------------------------
    // FA07 paso 2: médicos disponibles
    // ---------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<MedicoDisponibleDTO> medicosDisponiblesParaReasignacion(Integer idSucursal, Integer idEspecialidad,
                                                                        Integer idMedicoActual, Integer sucursalScope) {
        if (sucursalScope != null && !sucursalScope.equals(idSucursal)) {
            throw new ValidationException("No tiene permiso para consultar médicos de otra sucursal.");
        }
        return usuarioRepository
                .findByRol_NombreAndSucursal_IdAndEspecialidad_IdAndEstado("Médico", idSucursal, idEspecialidad, (short) 1)
                .stream()
                .filter(u -> !u.getId().equals(idMedicoActual))
                .map(u -> MedicoDisponibleDTO.builder()
                        .id(u.getId())
                        .nombreCompleto(u.getNombreCompleto())
                        .build())
                .toList();
    }

    // ---------------------------------------------------------------
    private CitaRecepcionDTO toDTO(Cita c) {
        return CitaRecepcionDTO.builder()
                .id(c.getId())
                .nombrePaciente(c.getPaciente().getNombreCompleto())
                .dpiPaciente(c.getPaciente().getDpi())
                .estado(c.getEstadoCita().getNombre())
                .esEmergencia(c.isEsEmergencia())
                .especialidad(c.getEspecialidad().getNombre())
                .sucursal(c.getSucursal().getNombre())
                .sucursalId(c.getSucursal().getId())
                .especialidadId(c.getEspecialidad().getId())
                .fechaHora(c.getFechaHora())
                .motivoConsulta(c.getMotivoConsulta())
                .horaLlegada(c.getHoraLlegada())
                .nombreMedico(c.getMedico().getNombreCompleto())
                .medicoId(c.getMedico().getId())
                .build();
    }
}