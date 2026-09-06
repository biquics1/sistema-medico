package com.example.demo.service;

import com.example.demo.dto.CitaRecepcionDTO;
import com.example.demo.dto.RecepcionDTOs.*;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.ValidationException;
import com.example.demo.modelo.Cita;
import com.example.demo.modelo.EstadoCita;
import com.example.demo.modelo.ReasignacionMedico;
import com.example.demo.modelo.Usuario;
import com.example.demo.repository.CitaRepository;
import com.example.demo.repository.EstadoCitaRepository;
import com.example.demo.repository.ReasignacionMedicoRepository;
import com.example.demo.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class RecepcionService {

    private static final Pattern DPI_PATTERN = Pattern.compile("^\\d{13}$");

    private final CitaRepository citaRepository;
    private final UsuarioRepository usuarioRepository;
    private final EstadoCitaRepository estadoCitaRepository;
    private final ReasignacionMedicoRepository reasignacionMedicoRepository;

    // ---------------------------------------------------------------
    // Paso 3 FB: búsqueda por número de cita o DPI (RN-CU05-01)
    // sucursalScope != null -> Administrador de Sede o Recepcionista: solo ve
    // citas de su propia sucursal. null -> Administrador General: sin
    // restricción.
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

        List<Cita> citas = citaRepository.buscarPorDpiPaciente(dpi.trim())
                .stream().filter(c -> perteneceASede(c, sucursalScope)).toList();
        if (!citas.isEmpty()) {
            return BusquedaResultadoDTO.builder()
                    .citaEncontrada(true)
                    .pacienteExiste(true)
                    .cita(toDTO(elegirCitaMasRelevante(citas)))
                    .mensaje(null)
                    .accionSugerida(null)
                    .build();
        }

        // Sin citas: distinguir FA03 (no existe el paciente) de FA04 (existe sin citas)
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

        return BusquedaResultadoDTO.builder()
                .citaEncontrada(false)
                .pacienteExiste(true)
                .cita(null)
                .mensaje("El paciente está registrado pero no tiene citas activas. Puede crear una nueva cita para este paciente.")
                .accionSugerida("NUEVA_CITA_WALKIN")
                .build();
    }

    // CORREGIDO: buscarPorDpiPaciente ordena las citas activas por fechaHora
    // DESC, así que si el paciente tenía más de una cita activa (ej. la de
    // hoy, ya con llegada registrada, y otra futura todavía "Confirmada"),
    // se devolvía la MÁS FUTURA en vez de la de HOY. Con dos personas
    // (recepción y luego admin) buscando al mismo paciente, cada una podía
    // terminar registrando la llegada de una cita DISTINTA sin darse cuenta,
    // pareciendo "doble registro" cuando en realidad cada una era válida
    // para una cita diferente. Ahora, si existe una cita de hoy entre las
    // activas, esa es la que se muestra; si no hay ninguna de hoy, se
    // mantiene el comportamiento anterior (la más próxima).
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

        // FA06
        if (EstadoCita.CANCELADA.equals(estadoActual)) {
            throw new ValidationException("La cita fue cancelada. El paciente debe agendar una nueva cita.");
        }
        // FA05
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

            String nombre = actualizada.getPaciente().getNombreCompleto();
            String mensaje = actualizada.isEsEmergencia()
                    // FA08
                    ? "Paciente " + nombre + " registrado con prioridad de EMERGENCIA. "
                    + "El paciente debe pasar directamente a toma de signos vitales."
                    : "La llegada del paciente " + nombre + " ha sido registrada exitosamente. "
                    + "El paciente debe pasar a la sala de espera.";

            return RegistrarLlegadaResponseDTO.builder()
                    .mensaje(mensaje)
                    .cita(toDTO(actualizada))
                    .build();

        } catch (ObjectOptimisticLockingFailureException e) {
            // FA09
            throw new ValidationException("Operación no permitida");
        }
    }

    // ---------------------------------------------------------------
    // NUEVO — Cancelación de cita desde Recepción (a solicitud del paciente
    // o por indicación administrativa). Solo procede mientras la cita no ha
    // iniciado el proceso clínico: se permite en "Pendiente de pago",
    // "Confirmada" y "Paciente Presente". Una vez que enfermería la pasó a
    // "Signos Vitales" (o estados posteriores), ya no se cancela desde aquí.
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

            String nombre = actualizada.getPaciente().getNombreCompleto();
            String mensaje = "La cita #" + actualizada.getId() + " del paciente " + nombre
                    + " ha sido cancelada correctamente.";

            return CancelarCitaResponseDTO.builder()
                    .mensaje(mensaje)
                    .cita(toDTO(actualizada))
                    .build();
        } catch (ObjectOptimisticLockingFailureException e) {
            // Mismo criterio que registrarLlegada(): otro usuario modificó la
            // cita (rowVersion) entre que se leyó y se intentó guardar.
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

        // CORREGIDO: antes se aceptaba cualquier idMedicoNuevo sin validar
        // que fuera realmente un Médico activo de la misma especialidad y
        // sucursal de la cita (FA07: "lista de médicos disponibles de la
        // misma sede y especialidad"). El endpoint de "médicos disponibles"
        // sí filtraba correctamente, pero nada impedía llamar directamente
        // a este endpoint con cualquier otro id de usuario.
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

        return toDTO(actualizada);
    }

    // ---------------------------------------------------------------
    // FA07 paso 2: médicos disponibles (misma sede + especialidad, activos)
    // Reutiliza el método que YA tienes en UsuarioRepository.
    // ---------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<MedicoDisponibleDTO> medicosDisponiblesParaReasignacion(Integer idSucursal, Integer idEspecialidad,
                                                                        Integer idMedicoActual, Integer sucursalScope) {
        if (sucursalScope != null && !sucursalScope.equals(idSucursal)) {
            // Administrador de Sede intentando consultar médicos de otra sucursal.
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