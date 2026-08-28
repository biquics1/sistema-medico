package com.example.demo.service;

import com.example.demo.dto.ConsultaMedicaDTOs.*;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.ValidationException;
import com.example.demo.modelo.*;
import com.example.demo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConsultaMedicaService {
    private final AuditoriaContexto auditoriaContexto;

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
    // Paso 1 FB: panel del médico agrupado en 3 secciones
    // ---------------------------------------------------------------
    @Transactional(readOnly = true)
    public PanelMedicoDTO obtenerPanel(Integer medicoId) {
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
    // Paso 2 FB: "Iniciar Consulta" -> En Espera -> Consulta Médica
    // ---------------------------------------------------------------
    @Transactional
    public IniciarConsultaResponseDTO iniciarConsulta(Integer idCita, Integer medicoId) {
        auditoriaContexto.aplicar();
        Cita cita = citaRepository.findByIdAndMedico_Id(idCita, medicoId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la cita indicada para este médico."));

        if (!EstadoCita.EN_ESPERA.equals(cita.getEstadoCita().getNombre())) {
            throw new ValidationException("La cita no se encuentra en estado 'En Espera'.");
        }

        EstadoCita consultaMedica = estadoCitaRepository.findByNombre(EstadoCita.CONSULTA_MEDICA)
                .orElseThrow(() -> new ResourceNotFoundException("Catálogo estado_cita incompleto: falta 'Consulta Médica'."));

        cita.setEstadoCita(consultaMedica);
        Cita actualizada = citaRepository.save(cita);

        String mensaje = String.format(
                "Turno número %d. Paciente %s, favor pasar a consulta médica.",
                actualizada.getId(), actualizada.getPaciente().getNombreCompleto());

        return IniciarConsultaResponseDTO.builder()
                .mensajeAnuncio(mensaje)
                .cita(toPanelDTO(actualizada))
                .build();
    }

    // ---------------------------------------------------------------
    // Paso 3 FB: contexto del formulario (crea o reabre el borrador)
    // ---------------------------------------------------------------
    @Transactional(readOnly = true)
    public ConsultaContextoDTO obtenerContexto(Integer idCita, Integer medicoId) {
        Cita cita = citaRepository.findByIdAndMedico_Id(idCita, medicoId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la cita indicada para este médico."));

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
    // Pasos 4-9 FB: guardar consulta (borrador "En curso" o cierre "Finalizada")
    // RN-CU08-01, RN-CU08-02, FA05
    // ---------------------------------------------------------------
    @Transactional
    public GuardarConsultaResponseDTO guardarConsulta(Integer idCita, Integer medicoId, GuardarConsultaRequestDTO req) {
        auditoriaContexto.aplicar();
        Cita cita = citaRepository.findByIdAndMedico_Id(idCita, medicoId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la cita indicada para este médico."));

        if (!EstadoCita.CONSULTA_MEDICA.equals(cita.getEstadoCita().getNombre())) {
            throw new ValidationException("La consulta no está disponible para edición en el estado actual de la cita.");
        }
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

        consulta.setMotivoVisita(req.getMotivoVisita());
        consulta.setHallazgosClinicos(req.getHallazgosClinicos());
        consulta.setDiagnostico(req.getDiagnostico());
        consulta.setPlanTratamiento(req.getPlanTratamiento());
        consulta.setNotasAdicionales(req.getNotasAdicionales());

        if (req.getCie10Id() != null) {
            Cie10 cie10 = cie10Repository.findById(req.getCie10Id())
                    .orElseThrow(() -> new ResourceNotFoundException("El código CIE-10 indicado no existe."));
            consulta.setCie10(cie10);
        } else {
            consulta.setCie10(null);
        }

        boolean finalizar = "FINALIZADA".equalsIgnoreCase(req.getEstadoConsulta());

        if (finalizar) {
            // RN-CU08-01 / FA05
            String diagnostico = req.getDiagnostico();
            if (diagnostico == null || diagnostico.isBlank()) {
                throw new ValidationException(
                        "No es posible finalizar la consulta sin registrar un diagnóstico. El campo Diagnóstico es obligatorio.");
            }
            if (diagnostico.length() < 10 || diagnostico.length() > 5000) {
                throw new ValidationException(
                        "El diagnóstico es obligatorio. Debe contener entre 10 y 5000 caracteres.");
            }
            // RN-CU08-02: el resto de campos obligatorios para el cierre
            if (isBlank(req.getHallazgosClinicos()) || isBlank(req.getPlanTratamiento())) {
                throw new ValidationException("Debe completar todos los campos obligatorios para cerrar la consulta.");
            }
            consulta.setFinalizada(true);
        }

        ConsultaMedica consultaGuardada = consultaMedicaRepository.save(consulta);

        String estadoResultante = cita.getEstadoCita().getNombre();
        if (finalizar) {
            EstadoCita evaluado = estadoCitaRepository.findByNombre(EstadoCita.EVALUADO)
                    .orElseThrow(() -> new ResourceNotFoundException("Catálogo estado_cita incompleto: falta 'Evaluado'."));
            cita.setEstadoCita(evaluado);
            citaRepository.save(cita);
            estadoResultante = evaluado.getNombre();
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
    // FA06: paciente no se presenta -> En Espera -> No Asistió
    // ---------------------------------------------------------------
    @Transactional
    public AccionCitaResponseDTO marcarNoAsistio(Integer idCita, Integer medicoId) {
        auditoriaContexto.aplicar();
        Cita cita = citaRepository.findByIdAndMedico_Id(idCita, medicoId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la cita indicada para este médico."));

        if (!EstadoCita.EN_ESPERA.equals(cita.getEstadoCita().getNombre())) {
            throw new ValidationException("Solo se puede marcar 'No Asistió' desde el estado 'En Espera'.");
        }

        EstadoCita noAsistio = estadoCitaRepository.findByNombre(EstadoCita.NO_ASISTIO)
                .orElseThrow(() -> new ResourceNotFoundException("Catálogo estado_cita incompleto: falta 'No Asistió'."));
        cita.setEstadoCita(noAsistio);
        Cita actualizada = citaRepository.save(cita);

        return AccionCitaResponseDTO.builder()
                .mensaje("Cita #" + actualizada.getId() + " marcada como No Asistió.")
                .cita(toPanelDTO(actualizada))
                .build();
    }

    // ---------------------------------------------------------------
    // Sección "Evaluados": Finalizar Atención -> Atención Finalizada
    // ---------------------------------------------------------------
    @Transactional
    public AccionCitaResponseDTO finalizarAtencion(Integer idCita, Integer medicoId) {
        auditoriaContexto.aplicar();
        Cita cita = citaRepository.findByIdAndMedico_Id(idCita, medicoId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la cita indicada para este médico."));

        if (!EstadoCita.EVALUADO.equals(cita.getEstadoCita().getNombre())) {
            throw new ValidationException("Solo se puede finalizar la atención de citas en estado 'Evaluado'.");
        }

        EstadoCita atencionFinalizada = estadoCitaRepository.findByNombre(EstadoCita.ATENCION_FINALIZADA)
                .orElseThrow(() -> new ResourceNotFoundException("Catálogo estado_cita incompleto: falta 'Atención Finalizada'."));
        cita.setEstadoCita(atencionFinalizada);
        Cita actualizada = citaRepository.save(cita);

        return AccionCitaResponseDTO.builder()
                .mensaje("Atención finalizada para cita #" + actualizada.getId() + ".")
                .cita(toPanelDTO(actualizada))
                .build();
    }

    // ---------------------------------------------------------------
    // Paso 6 FB: autocompletado CIE-10
    // ---------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<Cie10DTO> buscarCie10(String texto) {
        if (texto == null || texto.isBlank()) return List.of();
        return cie10Repository.buscar(texto.trim()).stream()
                .limit(20)
                .map(c -> Cie10DTO.builder().id(c.getId()).codigo(c.getCodigo()).descripcion(c.getDescripcion()).build())
                .toList();
    }

    // ---------------------------------------------------------------
    // FA01: generar orden de laboratorio
    // ---------------------------------------------------------------
    @Transactional
    public OrdenLaboratorioResponseDTO generarOrdenLaboratorio(Integer consultaId, Integer medicoId, OrdenLaboratorioRequestDTO req) {
        auditoriaContexto.aplicar();
        ConsultaMedica consulta = consultaMedicaRepository.findById(consultaId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la consulta indicada."));
        if (!consulta.getMedico().getId().equals(medicoId)) {
            throw new ResourceNotFoundException("No se encontró la consulta indicada para este médico.");
        }
        if (req.getExamenIds() == null || req.getExamenIds().isEmpty()) {
            throw new ValidationException("Debe seleccionar al menos un examen de laboratorio.");
        }

        List<ExamenLaboratorio> examenes = examenLaboratorioRepository.findAllById(req.getExamenIds());
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
        orden.setEsExterna(req.isEsExterna());
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
    // FA04: generar receta médica (RN-CU08-03)
    // ---------------------------------------------------------------
    @Transactional
    public RecetaResponseDTO generarReceta(Integer consultaId, Integer medicoId, RecetaRequestDTO req) {
        auditoriaContexto.aplicar();
        ConsultaMedica consulta = consultaMedicaRepository.findById(consultaId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la consulta indicada."));
        if (!consulta.getMedico().getId().equals(medicoId)) {
            throw new ResourceNotFoundException("No se encontró la consulta indicada para este médico.");
        }
        if (req.getDetalles() == null || req.getDetalles().isEmpty()) {
            throw new ValidationException("Debe agregar al menos un medicamento a la receta.");
        }

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
        for (DetalleRecetaRequestDTO d : req.getDetalles()) {
            Medicamento medicamento = medicamentoRepository.findById(d.getMedicamentoId())
                    .orElseThrow(() -> new ResourceNotFoundException("El medicamento indicado no existe en el catálogo."));

            DetalleRecetaMedica detalle = new DetalleRecetaMedica();
            detalle.setReceta(recetaGuardada);
            detalle.setMedicamento(medicamento);
            detalle.setDosis(d.getDosis());
            detalle.setFrecuencia(d.getFrecuencia());
            detalle.setDuracion(d.getDuracion());
            detalle.setIndicaciones(d.getIndicaciones());
            detalleRecetaMedicaRepository.save(detalle);

            nombresMedicamentos.add(medicamento.getNombre());
        }

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
                .build();
    }
}