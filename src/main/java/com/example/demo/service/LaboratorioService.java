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

// ============================================================================
// CU-09 GESTIÓN DE LABORATORIO - lógica y VALIDACIONES  (9_CU_Gestion_de_Laboratorio.docx, RN-CU09-01/02)
// Cómo buscar en este archivo (Ctrl+F / Ctrl+Shift+F):
//   >>> INICIO FB     = donde empieza cada paso del Flujo Básico
//   >>> INICIO FA     = donde empieza cada Flujo Alterno
//   >>> VALIDACION    = donde se valida una regla de negocio (RN) o una precondición
//
// MAPA DE MÉTODOS
//   FB paso 1       listarOrdenes()        (FA01: esExterna viaja en OrdenListaDTO)
//   FB pasos 2-3    obtenerDetalle()
//   FB pasos 5-7    NO están aquí: el cobro es CajaService.cobrarLaboratorio (CU-10); aquí solo se exige estado 'En proceso'
//   FB pasos 9-10   registrarResultado()   (FA02 fuera de rango se marca aquí)
//   FB pasos 11-14  publicarResultado()
// NO IMPLEMENTADO (pedido por RN-CU09-02 / RNF-024): validación de supervisor antes de publicar; exigir resultados de TODOS los exámenes.
// ============================================================================
@Service
@RequiredArgsConstructor
public class LaboratorioService {

    private final OrdenLaboratorioRepository ordenLaboratorioRepository;
    private final DetalleOrdenLaboratorioRepository detalleOrdenLaboratorioRepository;
    private final AuditoriaContexto auditoriaContexto;
    private final EmailService emailService;

    // ---------------------------------------------------------------
    // >>> INICIO FB - PASO 1: listado con filtros por estado, paciente y médico.
    // (FA01: las órdenes externas salen con esExterna = true; la etiqueta 'Externa' la dibuja el front)
    // sucursalScope != null -> Administrador de Sede o Laboratorista: solo
    // órdenes cuyo médico solicitante pertenece a su sucursal (orden_laboratorio
    // no tiene sucursal_id propio, se resuelve vía orden.medico.sucursal).
    // null -> Administrador General: sin restricción.
    // ---------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<OrdenListaDTO> listarOrdenes(Short estado, Integer pacienteId, Integer medicoId, Integer sucursalScope) {
        // >>> VALIDACION (filtros/acceso): estado, paciente y médico son filtros opcionales; perteneceASede() limita al Admin de Sede / Laboratorista a su sucursal.
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
    // >>> INICIO FB - PASOS 2 y 3: detalle de una orden (resumen + exámenes con su monto)
    // ---------------------------------------------------------------
    @Transactional(readOnly = true)
    public OrdenDetalleDTO obtenerDetalle(Integer idOrden, Integer sucursalScope) {
        // >>> VALIDACION: la orden debe existir y pertenecer a la sede del usuario (si no -> 404).
        OrdenLaboratorio orden = ordenLaboratorioRepository.findById(idOrden)
                .filter(o -> perteneceASede(o, sucursalScope))
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la orden de laboratorio indicada."));
        return toDetalleDTO(orden);
    }

    // ---------------------------------------------------------------
    // >>> INICIO FB - PASOS 9 y 10: registrar el resultado de un examen (RN-CU09-02)
    // Precondición: la orden debe estar "En proceso" (pago ya realizado, RN-CU09-01)
    // ---------------------------------------------------------------
    @Transactional
    public ResultadoResponseDTO registrarResultado(Integer idOrden, Integer idDetalle, ResultadoRequestDTO req,
                                                   Integer sucursalScope) {
        auditoriaContexto.aplicar();
        // >>> VALIDACION: la orden debe existir y pertenecer a la sede del usuario (si no -> 404).
        OrdenLaboratorio orden = ordenLaboratorioRepository.findById(idOrden)
                .filter(o -> perteneceASede(o, sucursalScope))
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la orden de laboratorio indicada."));

        // >>> VALIDACION RN-CU09-01 (FB pasos 5-7): sin pago (orden no 'En proceso') no se registran resultados ni hay toma de muestras.
        if (orden.getEstado() != OrdenLaboratorio.EN_PROCESO) {
            throw new ValidationException(
                    "El pago debe estar completado antes de proceder con la toma de muestras.");
        }

        // >>> VALIDACION: el examen (detalle) debe pertenecer a esta orden.
        DetalleOrdenLaboratorio detalle = detalleOrdenLaboratorioRepository.findByIdAndOrden_Id(idDetalle, idOrden)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el examen indicado en esta orden."));

        // >>> VALIDACION RN-CU09-02 / RNF-024: un resultado ya publicado no se puede modificar.
        if (detalle.isPublicado()) {
            throw new ValidationException("El resultado ya fue publicado y no puede modificarse.");
        }
        // >>> VALIDACION RN-CU09-02 (FB paso 10): el valor del resultado es obligatorio. Unidad, notas y fecha son opcionales (fecha null -> now).
        if (req.getValorResultado() == null || req.getValorResultado().isBlank()) {
            throw new ValidationException("Debe ingresar el valor del resultado.");
        }

        // >>> FB PASO 9: campos del formulario: Valor del Resultado, Unidad, Fecha del Resultado, 'Fuera de Rango' y Notas del Resultado.
        detalle.setValorResultado(req.getValorResultado().trim());
        detalle.setUnidad(req.getUnidad());
        // >>> INICIO FA02 - Resultado fuera de rango: la casilla la marca el laboratorista (NO se compara solo contra rangoReferencia).
        // El badge rojo 'Fuera de rango' lo dibuja el front con este flag. Se guarda normal y se continúa en la publicación (FB paso 11).
        detalle.setFueraDeRango(req.isFueraDeRango());
        detalle.setNotasResultado(req.getNotasResultado());
        detalle.setFechaResultado(req.getFechaResultado() != null ? req.getFechaResultado() : LocalDateTime.now());

        // >>> FB PASO 10: guarda el resultado y responde 'Resultado guardado exitosamente.'
        DetalleOrdenLaboratorio guardado = detalleOrdenLaboratorioRepository.save(detalle);

        return ResultadoResponseDTO.builder()
                .mensaje("Resultado guardado exitosamente.")
                .examen(toExamenDTO(guardado))
                .build();
    }

    // ---------------------------------------------------------------
    // >>> INICIO FB - PASOS 11 a 13: publicación individual del resultado (botón 'Publicar resultado' por examen)
    // ---------------------------------------------------------------
    @Transactional
    public PublicarResponseDTO publicarResultado(Integer idOrden, Integer idDetalle, Integer sucursalScope) {
        auditoriaContexto.aplicar();
        // >>> VALIDACION: la orden debe existir y pertenecer a la sede del usuario (si no -> 404). (No se exige estado 'En proceso' para publicar.)
        OrdenLaboratorio orden = ordenLaboratorioRepository.findById(idOrden)
                .filter(o -> perteneceASede(o, sucursalScope))
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la orden de laboratorio indicada."));

        // >>> VALIDACION: el examen (detalle) debe pertenecer a esta orden.
        DetalleOrdenLaboratorio detalle = detalleOrdenLaboratorioRepository.findByIdAndOrden_Id(idDetalle, idOrden)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el examen indicado en esta orden."));

        // >>> VALIDACION: no se puede publicar un examen sin resultado guardado (el paso 10 va antes del 11).
        if (detalle.getValorResultado() == null || detalle.getValorResultado().isBlank()) {
            throw new ValidationException("Debe guardar el resultado del examen antes de publicarlo.");
        }
        // >>> VALIDACION: el mismo resultado no se publica dos veces.
        if (detalle.isPublicado()) {
            throw new ValidationException("Este resultado ya fue publicado anteriormente.");
        }

        // >>> FB PASO 12: marca el examen como publicado (el front muestra la etiqueta 'Publicado' y oculta el botón).
        detalle.setPublicado(true);
        detalleOrdenLaboratorioRepository.save(detalle);

        // >>> INICIO FB - PASO 14: si ya no quedan exámenes sin publicar, la orden pasa a Completada
        // (el paso 13 del CU = repetir 10-12 por cada examen; lo hace el front llamando de nuevo a los endpoints)
        List<DetalleOrdenLaboratorio> todos = detalleOrdenLaboratorioRepository.findByOrden_Id(idOrden);
        boolean todosPublicados = todos.stream().allMatch(DetalleOrdenLaboratorio::isPublicado);
        boolean seCompleto = false;
        if (todosPublicados && orden.getEstado() == OrdenLaboratorio.EN_PROCESO) {
            orden.setEstado(OrdenLaboratorio.COMPLETADA);
            ordenLaboratorioRepository.save(orden);
            seCompleto = true;
        }

        // >>> NOTA: el CU (paso 13) dice que NO hay notificación al médico; este código envía correo al PACIENTE. La notificación al médico (RNF-007) no existe.
        emailService.enviarResultadoLaboratorioPublicado(orden, detalle, seCompleto);

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