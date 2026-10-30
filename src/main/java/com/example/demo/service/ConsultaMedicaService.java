package com.example.demo.service;

import com.example.demo.dto.ConsultaMedicaDTOs.*;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.ValidationException;
import com.example.demo.modelo.*;
import com.example.demo.repository.*;
import com.example.demo.security.AuthUsuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

// ============================================================================
// CU-08 CONSULTA MÉDICA - lógica y VALIDACIONES  (8_CU_Consulta_Medica.docx, RN-CU08-01/02/03)
// Cómo buscar en este archivo (Ctrl+F / Ctrl+Shift+F):
//   >>> INICIO FB     = donde empieza cada paso del Flujo Básico
//   >>> INICIO FA     = donde empieza cada Flujo Alterno
//   >>> VALIDACION    = donde se valida una regla de negocio (RN) o una precondición
//
// MAPA DE MÉTODOS
//   FB paso 1       obtenerPanel()
//   FB paso 2       iniciarConsulta()            (extra: llamarDeNuevo(), máx. 3 llamados)
//   FB paso 3       obtenerContexto()
//   FB pasos 4-10   guardarConsulta()            (FA05 se dispara aquí)
//   FB paso 7       buscarCie10()
//   FB pasos 11-12  finalizarAtencion()
//   FA01            generarOrdenLaboratorio()    (catálogo: listarExamenes())
//   FA02            NO está aquí: es SeguimientoService (CU-12)
//   FA04            generarReceta()              (catálogo: listarMedicamentos())
//   FA05            dentro de guardarConsulta()  (bloque 'if (finalizar)')
//   FA06            marcarNoAsistio()
//   Transversal     obtenerCitaComoMedico() / obtenerConsultaComoMedico()  (propiedad médico-cita)
// ============================================================================
@Service
@RequiredArgsConstructor
public class ConsultaMedicaService {
    private final AuditoriaContexto auditoriaContexto;
    private final EmailService emailService;

    private final CitaRepository citaRepository;
    private final EstadoCitaRepository estadoCitaRepository;
    private final ConsultaMedicaRepository consultaMedicaRepository;
    private final Cie10Repository cie10Repository;
    private final RecetaMedicaRepository recetaMedicaRepository;
    private final DetalleRecetaMedicaRepository detalleRecetaMedicaRepository;
    private final MedicamentoRepository medicamentoRepository;
    private final OrdenLaboratorioRepository ordenLaboratorioRepository;
    private final DetalleOrdenLaboratorioRepository detalleOrdenLaboratorioRepository;
    private final ExamenLaboratorioRepository examenLaboratorioRepository;

    // ---------------------------------------------------------------
    // >>> INICIO FB - PASO 1: panel del médico agrupado en 3 secciones (En Espera / En Consulta Médica / Evaluados)
    // ---------------------------------------------------------------
    @Transactional(readOnly = true)
    public PanelMedicoDTO obtenerPanel(AuthUsuario usuario) {
        // Administrador General: ve el panel consolidado de TODOS los médicos
        // (rol de supervisión/soporte). Médico: solo sus propias citas.
        // >>> VALIDACION (acceso): el Administrador General ve las citas de TODOS los médicos; el médico solo las suyas (su id sale del JWT).
        if (usuario.esAdminGeneral()) {
            List<CitaPanelDTO> enEspera = citaRepository
                    .findByEstadoCita_NombreOrderByFechaHoraAsc(EstadoCita.EN_ESPERA)
                    .stream().map(this::toPanelDTO).toList();
            List<CitaPanelDTO> enConsulta = citaRepository
                    .findByEstadoCita_NombreOrderByFechaHoraAsc(EstadoCita.CONSULTA_MEDICA)
                    .stream().map(this::toPanelDTO).toList();
            List<CitaPanelDTO> evaluados = citaRepository
                    .findByEstadoCita_NombreOrderByFechaHoraAsc(EstadoCita.EVALUADO)
                    .stream().map(this::toPanelDTO).toList();
            return PanelMedicoDTO.builder().enEspera(enEspera).enConsulta(enConsulta).evaluados(evaluados).build();
        }

        Integer medicoId = usuario.getId();
        List<CitaPanelDTO> enEspera = citaRepository
                .findByMedico_IdAndEstadoCita_NombreOrderByFechaHoraAsc(medicoId, EstadoCita.EN_ESPERA)
                .stream().map(this::toPanelDTO).toList();

        List<CitaPanelDTO> enConsulta = citaRepository
                .findByMedico_IdAndEstadoCita_NombreOrderByFechaHoraAsc(medicoId, EstadoCita.CONSULTA_MEDICA)
                .stream().map(this::toPanelDTO).toList();

        List<CitaPanelDTO> evaluados = citaRepository
                .findByMedico_IdAndEstadoCita_NombreOrderByFechaHoraAsc(medicoId, EstadoCita.EVALUADO)
                 .stream().map(this::toPanelDTO).toList();

        return PanelMedicoDTO.builder()
                .enEspera(enEspera)
                .enConsulta(enConsulta)
                .evaluados(evaluados)
                .build();
    }

    // ---------------------------------------------------------------
    // >>> INICIO FB - PASO 2: "Iniciar Consulta" -> En Espera -> Consulta Médica
    // ---------------------------------------------------------------
    @Transactional
    public IniciarConsultaResponseDTO iniciarConsulta(Integer idCita, AuthUsuario usuario) {
        auditoriaContexto.aplicar();
        Cita cita = obtenerCitaComoMedico(idCita, usuario);

        // >>> VALIDACION (precondición CU-08): la cita debe estar en estado 'En Espera' (signos vitales ya tomados).
        if (!EstadoCita.EN_ESPERA.equals(cita.getEstadoCita().getNombre())) {
            throw new ValidationException("La cita no se encuentra en estado 'En Espera'.");
        }

        EstadoCita consultaMedica = estadoCitaRepository.findByNombre(EstadoCita.CONSULTA_MEDICA)
                .orElseThrow(() -> new ResourceNotFoundException("Catálogo estado_cita incompleto: falta 'Consulta Médica'."));

        // >>> FB PASO 2 (continuación): cambia a 'Consulta Médica', cuenta el 1er llamado y arma el texto del anuncio TTS (la voz la reproduce el cliente).
        cita.setEstadoCita(consultaMedica);
        cita.setVecesLlamadoMedico((short) 1); // primer llamado de un máximo de 3
        Cita actualizada = citaRepository.save(cita);

        String mensaje = String.format(
                "Turno número %d. Paciente %s, favor pasar a consulta médica. (Llamado 1 de 3)",
                actualizada.getId(), actualizada.getPaciente().getNombreCompleto());

        return IniciarConsultaResponseDTO.builder()
                .mensajeAnuncio(mensaje)
                .cita(toPanelDTO(actualizada))
                .build();
    }

    // ---------------------------------------------------------------
    // >>> EXTRA (no está en el CU escrito) - "Llamar de nuevo":
    // "Llamar de nuevo": permite volver a anunciar al paciente sin
    // cambiar de estado, hasta un máximo de 3 llamados en total
    // (1 llamado inicial de "Iniciar Consulta" + 2 reintentos).
    // ---------------------------------------------------------------
    @Transactional
    public IniciarConsultaResponseDTO llamarDeNuevo(Integer idCita, AuthUsuario usuario) {
        auditoriaContexto.aplicar();
        Cita cita = obtenerCitaComoMedico(idCita, usuario);

        // >>> VALIDACION: solo se vuelve a llamar a un paciente que ya está en 'Consulta Médica'.
        if (!EstadoCita.CONSULTA_MEDICA.equals(cita.getEstadoCita().getNombre())) {
            throw new ValidationException("Solo se puede volver a llamar a un paciente que ya fue llamado a consulta.");
        }

        short vecesActuales = cita.getVecesLlamadoMedico() == null ? 1 : cita.getVecesLlamadoMedico();
        // >>> VALIDACION: máximo 3 llamados por cita; luego solo queda 'No Asistió' (FA06).
        if (vecesActuales >= 3) {
            throw new ValidationException(
                    "Se alcanzó el máximo de 3 llamados para esta cita. Puede marcar 'No Asistió' para cancelarla.");
        }

        short nuevasVeces = (short) (vecesActuales + 1);
        cita.setVecesLlamadoMedico(nuevasVeces);
        Cita actualizada = citaRepository.save(cita);

        String mensaje = String.format(
                "Turno número %d. Paciente %s, favor pasar a consulta médica. (Llamado %d de 3)",
                actualizada.getId(), actualizada.getPaciente().getNombreCompleto(), nuevasVeces);

        return IniciarConsultaResponseDTO.builder()
                .mensajeAnuncio(mensaje)
                .cita(toPanelDTO(actualizada))
                .build();
    }

    // ---------------------------------------------------------------
    // >>> INICIO FB - PASOS 3 y 4: 'Ver / Completar Consulta' -> contexto del formulario (crea o reabre el borrador).
    // El paso 4 (campos del formulario) se arma en el DTO ConsultaContextoDTO. Aquí no hay validaciones de negocio, solo la de propiedad médico-cita.
    // ---------------------------------------------------------------
    @Transactional(readOnly = true)
    public ConsultaContextoDTO obtenerContexto(Integer idCita, AuthUsuario usuario) {
        Cita cita = obtenerCitaComoMedico(idCita, usuario);

        return consultaMedicaRepository.findByCita_Id(idCita)
                .map(c -> ConsultaContextoDTO.builder()
                        .citaId(cita.getId())
                        .consultaId(c.getId())
                        .nombrePaciente(cita.getPaciente().getNombreCompleto())
                        .motivoVisita(c.getMotivoVisita())
                        .hallazgosClinicos(c.getHallazgosClinicos())
                        .cie10Id(c.getCie10() != null ? c.getCie10().getId() : null)
                        .cie10Codigo(c.getCie10() != null ? c.getCie10().getCodigo() : null)
                        .diagnostico(c.getDiagnostico())
                        .planTratamiento(c.getPlanTratamiento())
                        .notasAdicionales(c.getNotasAdicionales())
                        .finalizada(c.isFinalizada())
                        .build())
                .orElseGet(() -> ConsultaContextoDTO.builder()
                        .citaId(cita.getId())
                        .consultaId(null)
                        .nombrePaciente(cita.getPaciente().getNombreCompleto())
                        .motivoVisita(null)
                        .hallazgosClinicos(null)
                        .cie10Id(null)
                        .cie10Codigo(null)
                        .diagnostico(null)
                        .planTratamiento(null)
                        .notasAdicionales(null)
                        .finalizada(false)
                        .build());
    }

    // ---------------------------------------------------------------
    // >>> INICIO FB - PASOS 4 a 9: guardar consulta (borrador "En curso" o cierre "Finalizada").
    //   Paso 5 motivo | Paso 6 hallazgos | Paso 7 CIE-10 | Paso 8 diagnóstico y plan | Paso 9 estado + 'Guardar Consulta'
    // >>> INICIO FA05 (finalizar sin diagnóstico) está más abajo, dentro del bloque 'if (finalizar)'.
    // Reglas: RN-CU08-01 (diagnóstico) y RN-CU08-02 (campos obligatorios al cerrar).
    // ---------------------------------------------------------------
    @Transactional
    public GuardarConsultaResponseDTO guardarConsulta(Integer idCita, AuthUsuario usuario, GuardarConsultaRequestDTO req) {
        auditoriaContexto.aplicar();
        Cita cita = obtenerCitaComoMedico(idCita, usuario);

        // >>> VALIDACION (precondición): la consulta solo se edita/guarda si la cita está en 'Consulta Médica'.
        if (!EstadoCita.CONSULTA_MEDICA.equals(cita.getEstadoCita().getNombre())) {
            throw new ValidationException("La consulta no está disponible para edición en el estado actual de la cita.");
        }
        // >>> VALIDACION FB PASO 5 / RN-CU08-02: el motivo de visita es obligatorio (también para guardar borrador).
        if (req.getMotivoVisita() == null || req.getMotivoVisita().isBlank()) {
            throw new ValidationException("El motivo de visita es obligatorio.");
        }

        ConsultaMedica consulta = consultaMedicaRepository.findByCita_Id(idCita)
                .orElseGet(() -> {
                    ConsultaMedica nueva = new ConsultaMedica();
                    nueva.setCita(cita);
                    nueva.setMedico(cita.getMedico());
                    return nueva;
                });

        // >>> FB PASOS 5, 6 y 8: se copian al registro motivo, hallazgos clínicos, diagnóstico, plan y notas (si no se cierra, queda como borrador).
        consulta.setMotivoVisita(req.getMotivoVisita());
        consulta.setHallazgosClinicos(req.getHallazgosClinicos());
        consulta.setDiagnostico(req.getDiagnostico());
        consulta.setPlanTratamiento(req.getPlanTratamiento());
        consulta.setNotasAdicionales(req.getNotasAdicionales());

        // >>> FB PASO 7 + VALIDACION: el CIE-10 es opcional (RN-CU08-01); si viene un id, debe existir en el catálogo cie10 (si no -> 404).
        if (req.getCie10Id() != null) {
            Cie10 cie10 = cie10Repository.findById(req.getCie10Id())
                    .orElseThrow(() -> new ResourceNotFoundException("El código CIE-10 indicado no existe."));
            consulta.setCie10(cie10);
        } else {
            consulta.setCie10(null);
        }

        // >>> FB PASO 9: el médico elige estado 'Finalizada' (estadoConsulta = FINALIZADA) -> se activan las validaciones de cierre de abajo.
        boolean finalizar = "FINALIZADA".equalsIgnoreCase(req.getEstadoConsulta());

        if (finalizar) {
            // >>> INICIO FA05 - Intento de finalizar sin diagnóstico.
            // >>> VALIDACION RN-CU08-01: diagnóstico obligatorio para cerrar y de 10 a 5000 caracteres (2 ifs siguientes).
            String diagnostico = req.getDiagnostico();
            if (diagnostico == null || diagnostico.isBlank()) {
                throw new ValidationException(
                        "No es posible finalizar la consulta sin registrar un diagnóstico. El campo Diagnóstico es obligatorio.");
            }
            // >>> VALIDACION RN-CU08-01 (longitud): entre 10 y 5000 caracteres.
            if (diagnostico.length() < 10 || diagnostico.length() > 5000) {
                throw new ValidationException(
                        "El diagnóstico es obligatorio. Debe contener entre 10 y 5000 caracteres.");
            }
            // >>> VALIDACION RN-CU08-02: hallazgos clínicos y plan de tratamiento también son obligatorios para cerrar (el motivo ya se validó arriba).
            if (isBlank(req.getHallazgosClinicos()) || isBlank(req.getPlanTratamiento())) {
                throw new ValidationException("Debe completar todos los campos obligatorios para cerrar la consulta.");
            }
            consulta.setFinalizada(true);
        }

        ConsultaMedica consultaGuardada = consultaMedicaRepository.save(consulta);

        String estadoResultante = cita.getEstadoCita().getNombre();
        // >>> INICIO FB - PASO 10: consulta cerrada -> la cita pasa a 'Evaluado' (aparece en "Evaluados - Pendiente de cierre"), se envía correo y se devuelve el mensaje de éxito.
        if (finalizar) {
            EstadoCita evaluado = estadoCitaRepository.findByNombre(EstadoCita.EVALUADO)
                    .orElseThrow(() -> new ResourceNotFoundException("Catálogo estado_cita incompleto: falta 'Evaluado'."));
            cita.setEstadoCita(evaluado);
            citaRepository.save(cita);
            estadoResultante = evaluado.getNombre();
            emailService.enviarConsultaFinalizada(cita, consultaGuardada);
        }

        String mensaje = finalizar
                ? "La consulta ha sido finalizada exitosamente. El paciente puede proceder a las siguientes indicaciones médicas."
                : "Borrador de consulta guardado correctamente.";

        return GuardarConsultaResponseDTO.builder()
                .mensaje(mensaje)
                .consultaId(consultaGuardada.getId())
                .finalizada(consultaGuardada.isFinalizada())
                .estadoCita(estadoResultante)
                .build();
    }

    // ---------------------------------------------------------------
    // >>> INICIO FA06 - Paciente no asistió.
    // OJO: el CU escrito dice 'En Espera' -> 'No Asistió' (msg 'marcada como No Asistió'); este código exige 'Consulta Médica' y la pasa a 'Cancelada' (+ correo).
    // ---------------------------------------------------------------
    @Transactional
    public AccionCitaResponseDTO marcarNoAsistio(Integer idCita, AuthUsuario usuario) {
        auditoriaContexto.aplicar();
        Cita cita = obtenerCitaComoMedico(idCita, usuario);

        // >>> VALIDACION FA06: solo se marca 'No Asistió' a un paciente que ya fue llamado a consulta.
        if (!EstadoCita.CONSULTA_MEDICA.equals(cita.getEstadoCita().getNombre())) {
            throw new ValidationException("Solo se puede marcar 'No Asistió' para un paciente ya llamado a consulta.");
        }

        EstadoCita cancelada = estadoCitaRepository.findByNombre(EstadoCita.CANCELADA)
                .orElseThrow(() -> new ResourceNotFoundException("Catálogo estado_cita incompleto: falta 'Cancelada'."));
        cita.setEstadoCita(cancelada);
        Cita actualizada = citaRepository.save(cita);
        emailService.enviarCitaCancelada(actualizada, "No asistió a la consulta médica pese a los llamados.");

        return AccionCitaResponseDTO.builder()
                .mensaje("Cita #" + actualizada.getId() + " cancelada por inasistencia del paciente.")
                .cita(toPanelDTO(actualizada))
                .build();
    }

    // ---------------------------------------------------------------
    // >>> INICIO FB - PASOS 11 y 12: sección "Evaluados" -> 'Finalizar Atención' (Evaluado -> Atención Finalizada).
    // ---------------------------------------------------------------
    @Transactional
    public AccionCitaResponseDTO finalizarAtencion(Integer idCita, AuthUsuario usuario) {
        auditoriaContexto.aplicar();
        Cita cita = obtenerCitaComoMedico(idCita, usuario);

        // >>> VALIDACION: solo se finaliza la atención de citas en estado 'Evaluado' (no se exige que haya orden, receta o seguimiento).
        if (!EstadoCita.EVALUADO.equals(cita.getEstadoCita().getNombre())) {
            throw new ValidationException("Solo se puede finalizar la atención de citas en estado 'Evaluado'.");
        }

        EstadoCita atencionFinalizada = estadoCitaRepository.findByNombre(EstadoCita.ATENCION_FINALIZADA)
                .orElseThrow(() -> new ResourceNotFoundException("Catálogo estado_cita incompleto: falta 'Atención Finalizada'."));
        cita.setEstadoCita(atencionFinalizada);
        Cita actualizada = citaRepository.save(cita);
        emailService.enviarAtencionFinalizada(actualizada);

        return AccionCitaResponseDTO.builder()
                .mensaje("Atención finalizada para cita #" + actualizada.getId() + ".")
                .cita(toPanelDTO(actualizada))
                .build();
    }

    // ---------------------------------------------------------------
    // >>> INICIO FB - PASO 7: autocompletado CIE-10 (el CU escrito lo numera como paso 7).
    // ---------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<Cie10DTO> buscarCie10(String texto) {
        // >>> VALIDACION: texto vacío -> devuelve lista vacía (no lanza error). Máx. 20 sugerencias.
        if (texto == null || texto.isBlank()) return List.of();
        return cie10Repository.buscar(texto.trim()).stream()
                .limit(20)
                .map(c -> Cie10DTO.builder().id(c.getId()).codigo(c.getCodigo()).descripcion(c.getDescripcion()).build())
                .toList();
    }

    // ---------------------------------------------------------------
    // >>> INICIO FA01 - El paciente requiere exámenes: generar orden de laboratorio (se usa desde la sección "Evaluados"; luego se continúa en el paso 11).
    // ---------------------------------------------------------------
    @Transactional
    public OrdenLaboratorioResponseDTO generarOrdenLaboratorio(Integer consultaId, AuthUsuario usuario, OrdenLaboratorioRequestDTO req) {
        auditoriaContexto.aplicar();
        // >>> VALIDACION: la consulta debe existir y ser de este médico (Admin General exceptuado). No se exige que esté 'Finalizada'.
        ConsultaMedica consulta = obtenerConsultaComoMedico(consultaId, usuario);
        // >>> VALIDACION FA01: debe seleccionar al menos un examen.
        if (req.getExamenIds() == null || req.getExamenIds().isEmpty()) {
            throw new ValidationException("Debe seleccionar al menos un examen de laboratorio.");
        }

        List<ExamenLaboratorio> examenes = examenLaboratorioRepository.findAllById(req.getExamenIds());
        // >>> VALIDACION FA01: todos los exámenes elegidos deben existir en el catálogo.
        if (examenes.size() != req.getExamenIds().size()) {
            throw new ValidationException("Uno o más exámenes seleccionados no existen en el catálogo.");
        }

        BigDecimal total = examenes.stream()
                .map(ExamenLaboratorio::getPrecio)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        OrdenLaboratorio orden = new OrdenLaboratorio();
        orden.setConsulta(consulta);
        orden.setPaciente(consulta.getCita().getPaciente());
        orden.setMedico(consulta.getMedico());
        // >>> FA01 de CU-09 nace aquí: la casilla 'Orden Externa' se guarda en la orden; las notas = observaciones del médico.
        orden.setEsExterna(req.isEsExterna());
        // >>> La orden nace 'Pendiente'; pasa a 'En proceso' al cobrarla en caja (CU-10) y se trabaja en CU-09.
        orden.setEstado(OrdenLaboratorio.PENDIENTE);
        orden.setMontoTotal(total);
        orden.setNotas(req.getNotas());
        OrdenLaboratorio ordenGuardada = ordenLaboratorioRepository.save(orden);

        for (ExamenLaboratorio examen : examenes) {
            DetalleOrdenLaboratorio detalle = new DetalleOrdenLaboratorio();
            detalle.setOrden(ordenGuardada);
            detalle.setExamen(examen);
            detalle.setPrecioUnitario(examen.getPrecio());
            detalleOrdenLaboratorioRepository.save(detalle);
        }

        List<String> nombres = examenes.stream().map(ExamenLaboratorio::getNombre).toList();
        // >>> FA01 (mensaje final): 'Orden de laboratorio generada exitosamente. Número de orden: ... El paciente debe dirigirse al área de laboratorio.'
        emailService.enviarOrdenLaboratorio(ordenGuardada, nombres);
        String mensaje = String.format(
                "Orden de laboratorio generada exitosamente. Número de orden: %d. Exámenes: %s. "
                        + "El paciente debe dirigirse al área de laboratorio.",
                ordenGuardada.getId(), String.join(", ", nombres));

        return OrdenLaboratorioResponseDTO.builder()
                .mensaje(mensaje)
                .numeroOrden(ordenGuardada.getId())
                .examenes(nombres)
                .montoTotal(total)
                .build();
    }

    // ---------------------------------------------------------------
    // >>> INICIO FA04 - El paciente requiere medicamentos: generar receta médica (RN-CU08-03). Luego se continúa en el paso 11.
    // La receta solo es despachable 7 días (RN-CU10-01, CU-11).
    // ---------------------------------------------------------------
    @Transactional
    public RecetaResponseDTO generarReceta(Integer consultaId, AuthUsuario usuario, RecetaRequestDTO req) {
        auditoriaContexto.aplicar();
        // >>> VALIDACION: la consulta debe existir y ser de este médico (Admin General exceptuado). No se exige que esté 'Finalizada'.
        ConsultaMedica consulta = obtenerConsultaComoMedico(consultaId, usuario);
        // >>> VALIDACION FA04: la receta debe llevar al menos un medicamento.
        if (req.getDetalles() == null || req.getDetalles().isEmpty()) {
            throw new ValidationException("Debe agregar al menos un medicamento a la receta.");
        }

        // >>> VALIDACION RN-CU08-03: medicamento, dosis, frecuencia y duración son obligatorios; 'indicaciones' es opcional.
        for (DetalleRecetaRequestDTO d : req.getDetalles()) {
            if (d.getMedicamentoId() == null || isBlank(d.getDosis()) || isBlank(d.getFrecuencia()) || isBlank(d.getDuracion())) {
                throw new ValidationException(
                        "Todos los campos de la receta son obligatorios excepto indicaciones especiales.");
            }
        }

        RecetaMedica receta = new RecetaMedica();
        receta.setConsulta(consulta);
        RecetaMedica recetaGuardada = recetaMedicaRepository.save(receta);

        List<String> nombresMedicamentos = new java.util.ArrayList<>();
        List<DetalleRecetaMedica> detallesGuardados = new java.util.ArrayList<>();
        for (DetalleRecetaRequestDTO d : req.getDetalles()) {
            // >>> VALIDACION FA04: el medicamento debe existir en el catálogo (solo se valida existencia; no estado activo ni dosis coherente).
            Medicamento medicamento = medicamentoRepository.findById(d.getMedicamentoId())
                    .orElseThrow(() -> new ResourceNotFoundException("El medicamento indicado no existe en el catálogo."));

            DetalleRecetaMedica detalle = new DetalleRecetaMedica();
            detalle.setReceta(recetaGuardada);
            detalle.setMedicamento(medicamento);
            detalle.setDosis(d.getDosis());
            detalle.setFrecuencia(d.getFrecuencia());
            detalle.setDuracion(d.getDuracion());
            detalle.setIndicaciones(d.getIndicaciones());
            detallesGuardados.add(detalleRecetaMedicaRepository.save(detalle));

            nombresMedicamentos.add(medicamento.getNombre());
        }

        // La receta se envía al correo del paciente al guardarla
        emailService.enviarReceta(recetaGuardada, detallesGuardados);

        String mensaje = String.format(
                "Receta médica generada exitosamente. Medicamentos: %s. El paciente puede adquirirlos en la farmacia de la clínica.",
                String.join(", ", nombresMedicamentos));

        return RecetaResponseDTO.builder()
                .mensaje(mensaje)
                .recetaId(recetaGuardada.getId())
                .medicamentos(nombresMedicamentos)
                .build();
    }

    // ---------------------------------------------------------------
    // Catálogos de apoyo para los formularios de FA01 y FA04
    // ---------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<ExamenCatalogoDTO> listarExamenes() {
        return examenLaboratorioRepository.findByEstado((short) 1).stream()
                .map(e -> ExamenCatalogoDTO.builder()
                        .id(e.getId()).codigo(e.getCodigo()).nombre(e.getNombre()).precio(e.getPrecio())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MedicamentoCatalogoDTO> listarMedicamentos() {
        return medicamentoRepository.findByEstado((short) 1).stream()
                .map(m -> MedicamentoCatalogoDTO.builder()
                        .id(m.getId()).nombre(m.getNombre()).unidad(m.getUnidad()).esControlado(m.isEsControlado())
                        .build())
                .toList();
    }

    // ---------------------------------------------------------------
    // >>> VALIDACION TRANSVERSAL (todos los pasos de CU-08): resuelve la cita respetando la propiedad médico-paciente. El
    // Administrador General puede operar sobre la cita de cualquier
    // médico (supervisión/soporte); el resto solo sobre las propias.
    // ---------------------------------------------------------------
    private Cita obtenerCitaComoMedico(Integer idCita, AuthUsuario usuario) {
        if (usuario.esAdminGeneral()) {
            return citaRepository.findById(idCita)
                    .orElseThrow(() -> new ResourceNotFoundException("No se encontró la cita indicada."));
        }
        return citaRepository.findByIdAndMedico_Id(idCita, usuario.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la cita indicada para este médico."));
    }

    private ConsultaMedica obtenerConsultaComoMedico(Integer idConsulta, AuthUsuario usuario) {
        ConsultaMedica consulta = consultaMedicaRepository.findById(idConsulta)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la consulta indicada."));
        if (!usuario.esAdminGeneral() && !consulta.getMedico().getId().equals(usuario.getId())) {
            throw new ResourceNotFoundException("No se encontró la consulta indicada para este médico.");
        }
        return consulta;
    }

    // ---------------------------------------------------------------
    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private CitaPanelDTO toPanelDTO(Cita c) {
        return CitaPanelDTO.builder()
                .id(c.getId())
                .nombrePaciente(c.getPaciente().getNombreCompleto())
                .especialidad(c.getEspecialidad().getNombre())
                .fechaHora(c.getFechaHora())
                .estado(c.getEstadoCita().getNombre())
                .esEmergencia(c.isEsEmergencia())
                .vecesLlamado(c.getVecesLlamadoMedico())
                .build();
    }
}