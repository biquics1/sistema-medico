package com.example.demo.service;

import com.example.demo.dto.LaboratorioDTOs.*;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.ValidationException;
import com.example.demo.modelo.DetalleOrdenLaboratorio;
import com.example.demo.modelo.OrdenLaboratorio;
import com.example.demo.repository.DetalleOrdenLaboratorioRepository;
import com.example.demo.repository.OrdenLaboratorioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LaboratorioService {

    private final OrdenLaboratorioRepository ordenLaboratorioRepository;
    private final DetalleOrdenLaboratorioRepository detalleOrdenLaboratorioRepository;
    private final AuditoriaContexto auditoriaContexto;

    // ---------------------------------------------------------------
    // Paso 1 FB: listado con filtros por estado, paciente y médico (FA01)
    // sucursalScope != null -> Administrador de Sede o Laboratorista: solo
    // órdenes cuyo médico solicitante pertenece a su sucursal (orden_laboratorio
    // no tiene sucursal_id propio, se resuelve vía orden.medico.sucursal).
    // null -> Administrador General: sin restricción.
    // ---------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<OrdenListaDTO> listarOrdenes(Short estado, Integer pacienteId, Integer medicoId, Integer sucursalScope) {
        return ordenLaboratorioRepository.findAllByOrderByCreadoEnDesc().stream()
                .filter(o -> estado == null || estado.equals(o.getEstado()))
                .filter(o -> pacienteId == null || pacienteId.equals(o.getPaciente().getId()))
                .filter(o -> medicoId == null || medicoId.equals(o.getMedico().getId()))
                .filter(o -> perteneceASede(o, sucursalScope))
                .map(this::toListaDTO)
                .toList();
    }

    private boolean perteneceASede(OrdenLaboratorio o, Integer sucursalScope) {
        if (sucursalScope == null) return true;
        var medico = o.getMedico();
        return medico != null && medico.getSucursal() != null && sucursalScope.equals(medico.getSucursal().getId());
    }

    // ---------------------------------------------------------------
    // Paso 2-3 FB: detalle de una orden
    // ---------------------------------------------------------------
    @Transactional(readOnly = true)
    public OrdenDetalleDTO obtenerDetalle(Integer idOrden, Integer sucursalScope) {
        OrdenLaboratorio orden = ordenLaboratorioRepository.findById(idOrden)
                .filter(o -> perteneceASede(o, sucursalScope))
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la orden de laboratorio indicada."));
        return toDetalleDTO(orden);
    }

    // ---------------------------------------------------------------
    // Pasos 9-10 FB: registrar resultado de un examen (RN-CU09-02)
    // Precondición: la orden debe estar "En proceso" (pago ya realizado, RN-CU09-01)
    // ---------------------------------------------------------------
    @Transactional
    public ResultadoResponseDTO registrarResultado(Integer idOrden, Integer idDetalle, ResultadoRequestDTO req,
                                                   Integer sucursalScope) {
        auditoriaContexto.aplicar();
        OrdenLaboratorio orden = ordenLaboratorioRepository.findById(idOrden)
                .filter(o -> perteneceASede(o, sucursalScope))
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la orden de laboratorio indicada."));

        if (orden.getEstado() != OrdenLaboratorio.EN_PROCESO) {
            throw new ValidationException(
                    "El pago debe estar completado antes de proceder con la toma de muestras.");
        }

        DetalleOrdenLaboratorio detalle = detalleOrdenLaboratorioRepository.findByIdAndOrden_Id(idDetalle, idOrden)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el examen indicado en esta orden."));

        if (detalle.isPublicado()) {
            throw new ValidationException("El resultado ya fue publicado y no puede modificarse.");
        }
        if (req.getValorResultado() == null || req.getValorResultado().isBlank()) {
            throw new ValidationException("Debe ingresar el valor del resultado.");
        }

        detalle.setValorResultado(req.getValorResultado().trim());
        detalle.setUnidad(req.getUnidad());
        detalle.setFueraDeRango(req.isFueraDeRango());
        detalle.setNotasResultado(req.getNotasResultado());
        detalle.setFechaResultado(req.getFechaResultado() != null ? req.getFechaResultado() : LocalDateTime.now());

        DetalleOrdenLaboratorio guardado = detalleOrdenLaboratorioRepository.save(detalle);

        return ResultadoResponseDTO.builder()
                .mensaje("Resultado guardado exitosamente.")
                .examen(toExamenDTO(guardado))
                .build();
    }

    // ---------------------------------------------------------------
    // Pasos 11-13 FB: publicación individual del resultado
    // ---------------------------------------------------------------
    @Transactional
    public PublicarResponseDTO publicarResultado(Integer idOrden, Integer idDetalle, Integer sucursalScope) {
        auditoriaContexto.aplicar();
        OrdenLaboratorio orden = ordenLaboratorioRepository.findById(idOrden)
                .filter(o -> perteneceASede(o, sucursalScope))
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la orden de laboratorio indicada."));

        DetalleOrdenLaboratorio detalle = detalleOrdenLaboratorioRepository.findByIdAndOrden_Id(idDetalle, idOrden)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el examen indicado en esta orden."));

        if (detalle.getValorResultado() == null || detalle.getValorResultado().isBlank()) {
            throw new ValidationException("Debe guardar el resultado del examen antes de publicarlo.");
        }
        if (detalle.isPublicado()) {
            throw new ValidationException("Este resultado ya fue publicado anteriormente.");
        }

        detalle.setPublicado(true);
        detalleOrdenLaboratorioRepository.save(detalle);

        // Paso 13 FB: si ya no quedan exámenes sin publicar, la orden se considera completada
        List<DetalleOrdenLaboratorio> todos = detalleOrdenLaboratorioRepository.findByOrden_Id(idOrden);
        boolean todosPublicados = todos.stream().allMatch(DetalleOrdenLaboratorio::isPublicado);
        boolean seCompleto = false;
        if (todosPublicados && orden.getEstado() == OrdenLaboratorio.EN_PROCESO) {
            orden.setEstado(OrdenLaboratorio.COMPLETADA);
            ordenLaboratorioRepository.save(orden);
            seCompleto = true;
        }

        return PublicarResponseDTO.builder()
                .mensaje("Resultado publicado exitosamente.")
                .examen(toExamenDTO(detalle))
                .ordenCompletada(seCompleto)
                .build();
    }

    // ---------------------------------------------------------------
    private String estadoTexto(short estado) {
        return switch (estado) {
            case OrdenLaboratorio.PENDIENTE -> "Pendiente";
            case OrdenLaboratorio.EN_PROCESO -> "En proceso";
            case OrdenLaboratorio.COMPLETADA -> "Completada";
            case OrdenLaboratorio.CANCELADA -> "Cancelada";
            default -> "Desconocido";
        };
    }

    private OrdenListaDTO toListaDTO(OrdenLaboratorio o) {
        List<DetalleOrdenLaboratorio> detalles = detalleOrdenLaboratorioRepository.findByOrden_Id(o.getId());
        return OrdenListaDTO.builder()
                .id(o.getId())
                .nombrePaciente(o.getPaciente().getNombreCompleto())
                .dpiPaciente(o.getPaciente().getDpi())
                .nombreMedico(o.getMedico().getNombreCompleto())
                .estadoCodigo(o.getEstado())
                .estado(estadoTexto(o.getEstado()))
                .esExterna(o.isEsExterna())
                .cantidadExamenes(detalles.size())
                .montoTotal(o.getMontoTotal())
                .creadoEn(o.getCreadoEn())
                .build();
    }

    private OrdenDetalleDTO toDetalleDTO(OrdenLaboratorio o) {
        List<ExamenOrdenDTO> examenes = detalleOrdenLaboratorioRepository.findByOrden_Id(o.getId()).stream()
                .map(this::toExamenDTO)
                .toList();
        return OrdenDetalleDTO.builder()
                .id(o.getId())
                .nombrePaciente(o.getPaciente().getNombreCompleto())
                .dpiPaciente(o.getPaciente().getDpi())
                .nombreMedico(o.getMedico().getNombreCompleto())
                .estadoCodigo(o.getEstado())
                .estado(estadoTexto(o.getEstado()))
                .esExterna(o.isEsExterna())
                .montoTotal(o.getMontoTotal())
                .notas(o.getNotas())
                .creadoEn(o.getCreadoEn())
                .examenes(examenes)
                .build();
    }

    private ExamenOrdenDTO toExamenDTO(DetalleOrdenLaboratorio d) {
        return ExamenOrdenDTO.builder()
                .id(d.getId())
                .examenId(d.getExamen().getId())
                .nombreExamen(d.getExamen().getNombre())
                .codigoExamen(d.getExamen().getCodigo())
                .precioUnitario(d.getPrecioUnitario())
                .rangoReferencia(d.getExamen().getRangoReferencia())
                .valorResultado(d.getValorResultado())
                .unidad(d.getUnidad())
                .fueraDeRango(d.isFueraDeRango())
                .notasResultado(d.getNotasResultado())
                .publicado(d.isPublicado())
                .fechaResultado(d.getFechaResultado())
                .build();
    }
}