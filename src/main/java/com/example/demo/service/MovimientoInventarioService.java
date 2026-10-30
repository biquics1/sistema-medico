package com.example.demo.service;

import com.example.demo.dto.MovimientoInventarioDTO;
import com.example.demo.dto.PageResponseDTO;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.ValidationException;
import com.example.demo.modelo.*;
import com.example.demo.repository.InventarioMedicamentoRepository;
import com.example.demo.repository.MedicamentoRepository;
import com.example.demo.repository.MovimientoInventarioRepository;
import com.example.demo.repository.MovimientoInventarioSpecifications;
import com.example.demo.repository.SucursalRepository;
import com.example.demo.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

// ============================================================================
// CU-15 BITÁCORA DE MOVIMIENTOS DE INVENTARIO - lógica y VALIDACIONES  (15_CU_Bitacora_Movimientos_Inventario.docx;
// en el código: CU-13, RN-CU13-01 / RN-CU13-02 / RNF-025)
// Cómo buscar en este archivo (Ctrl+F / Ctrl+Shift+F):
//   >>> INICIO FB     = donde empieza cada paso del Flujo Básico
//   >>> INICIO FA     = donde empieza cada Flujo Alterno
//   >>> VALIDACION    = donde se valida una regla de negocio (RN) o una precondición
//
// MAPA DE MÉTODOS
//   FB paso 2        listar()  y  cambiarEstado()  (acción 'Desactivar/Activar')
//   FB pasos 6-9     registrar()   (FA02 stock insuficiente se valida dentro)
//   FA03             validarCampos()  (+ anotaciones de MovimientoInventarioDTO.CreateDTO)
//   FA01 / FA04 / FA05  NO están aquí: son del front (tabla vacía, cancelar, alerta preventiva de stock mínimo)
// Tipos: 0 Compra, 1 Devolución, 2 Venta, 3 Reclamo, 4 Ajuste+, 5 Ajuste-, 6 Despacho (automático, lo crea FarmaciaService / CU-11).
// ============================================================================
@Service
@RequiredArgsConstructor
public class MovimientoInventarioService {

    private final MovimientoInventarioRepository repository;
    private final InventarioMedicamentoRepository inventarioRepository;
    private final MedicamentoRepository medicamentoRepository;
    private final SucursalRepository sucursalRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaContexto auditoriaContexto;

    private static final String[] NOMBRES_TIPO = {
            "Compra", "Devolución", "Venta", "Reclamo", "Ajuste+", "Ajuste-", "Despacho"
    };

    // >>> INICIO FB - PASO 2: listado paginado de movimientos (FA01: si no hay datos devuelve página vacía; el mensaje lo pone el front).
    // >>> VALIDACION (acceso): sucursalIdEs(sucursalScope) limita al Admin de Sede / Farmacéutico a su sucursal; null = Admin General.
    public PageResponseDTO<MovimientoInventarioDTO.ResponseDTO> listar(Pageable pageable, Integer sucursalScope) {
        Page<MovimientoInventario> page = repository.findAll(
                MovimientoInventarioSpecifications.sucursalIdEs(sucursalScope), pageable);

        List<MovimientoInventarioDTO.ResponseDTO> contenido = page.getContent().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());

        return new PageResponseDTO<>(contenido, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    // >>> INICIO FB - PASOS 6 a 9: 'Registrar Movimiento' (pasos 7 y 8: validar, registrar y actualizar el stock).
    @Transactional
    public MovimientoInventarioDTO.ResponseDTO registrar(MovimientoInventarioDTO.CreateDTO dto, Integer usuarioIdActual,
                                                         Integer sucursalScope) {
        auditoriaContexto.aplicar();
        // >>> VALIDACION FA03 (FB paso 7): reglas de campos de RN-CU13-01 -> ver validarCampos() al final del archivo.
        validarCampos(dto);
        // >>> VALIDACION (acceso): no se registran movimientos en otra sucursal.
        if (sucursalScope != null && !sucursalScope.equals(dto.getSucursalId())) {
            throw new ValidationException("No tiene permiso para registrar movimientos en otra sucursal.");
        }

        // >>> VALIDACION: medicamento, sucursal y usuario deben existir (3 búsquedas siguientes).
        Medicamento medicamento = medicamentoRepository.findById(dto.getMedicamentoId())
                .orElseThrow(() -> new ValidationException("Debe seleccionar un medicamento."));
        Sucursal sucursal = sucursalRepository.findById(dto.getSucursalId())
                .orElseThrow(() -> new ValidationException("Debe seleccionar una sucursal."));
        Usuario usuario = usuarioRepository.findById(usuarioIdActual)
                .orElseThrow(() -> new ValidationException("Usuario no válido."));

        // >>> FB PASO 8 (preparación): busca el inventario medicamento+sucursal; si no existe lo crea con stock 0.
        InventarioMedicamento inventario = inventarioRepository
                .findByMedicamentoIdAndSucursalId(dto.getMedicamentoId(), dto.getSucursalId())
                .orElseGet(() -> {
                    InventarioMedicamento nuevo = new InventarioMedicamento();
                    nuevo.setMedicamento(medicamento);
                    nuevo.setSucursal(sucursal);
                    nuevo.setStockActual(0);
                    return inventarioRepository.save(nuevo);
                });

        // >>> FB PASO 8: calcula el stock nuevo: las salidas (Venta, Reclamo, Ajuste-) restan; las entradas (Compra, Devolución, Ajuste+) suman.
        int stockAnterior = inventario.getStockActual();
        int stockNuevo;
        boolean esSalida = esTipoSalida(dto.getTipoMovimiento());

        if (esSalida) {
            // >>> INICIO FA02 - Salida (Venta, Reclamo o Ajuste-) con stock insuficiente.
            // >>> VALIDACION RN-CU13-02: la cantidad no puede exceder el stock actual -> 'Stock insuficiente. Stock actual: ...'
            if (dto.getCantidad() > stockAnterior) {
                throw new ValidationException("Stock insuficiente. Stock actual: " + stockAnterior
                        + ". No se puede registrar una salida de " + dto.getCantidad() + " unidades.");
            }
            stockNuevo = stockAnterior - dto.getCantidad();
        } else {
            stockNuevo = stockAnterior + dto.getCantidad();
        }

        // >>> FB PASO 8: se actualiza el stock en inventario_medicamento. RNF-025 / RN-CU13-02: control de concurrencia optimista (campo version);
        // si otro proceso lo modificó -> 'El inventario fue modificado por otra operación. Intente nuevamente.'
        inventario.setStockActual(stockNuevo);
        try {
            inventarioRepository.save(inventario);
        } catch (OptimisticLockingFailureException ex) {
            throw new ValidationException("El inventario fue modificado por otra operación. Intente nuevamente.");
        }

        // >>> FB PASO 7: se registra el movimiento en la bitácora con stock anterior/nuevo, usuario (del JWT), activo=true y fecha (creadoEn).
        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setMedicamento(medicamento);
        movimiento.setSucursal(sucursal);
        movimiento.setTipoMovimiento(dto.getTipoMovimiento());
        movimiento.setCantidad(dto.getCantidad());
        movimiento.setStockAnterior(stockAnterior);
        movimiento.setStockNuevo(stockNuevo);
        movimiento.setCostoUnitario(dto.getCostoUnitario());
        movimiento.setNumeroReferencia(dto.getNumeroReferencia());
        movimiento.setMotivo(dto.getMotivo());
        movimiento.setUsuario(usuario);
        movimiento.setActivo(true);

        // >>> FB PASO 9: devuelve el movimiento creado (HTTP 201). El texto 'Movimiento registrado exitosamente. Medicamento: ...' del CU NO se arma aquí.
        // FA05 (alerta preventiva de stock mínimo): no existe en backend.
        return toResponseDTO(repository.save(movimiento));
    }

    // >>> ACCIÓN POR FILA del FB PASO 2 ('Desactivar/Activar'): cambia solo el flag 'activo'; NO revierte el stock.
    @Transactional
    public MovimientoInventarioDTO.ResponseDTO cambiarEstado(Integer id, boolean activo, Integer sucursalScope) {
        auditoriaContexto.aplicar();
        MovimientoInventario movimiento = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movimiento no encontrado."));
        if (sucursalScope != null && !sucursalScope.equals(movimiento.getSucursal().getId())) {
            throw new ResourceNotFoundException("Movimiento no encontrado.");
        }
        movimiento.setActivo(activo);
        return toResponseDTO(repository.save(movimiento));
    }

    // >>> INICIO FA03 - Validación de campos fallida. VALIDACION RN-CU13-01, orden de ejecución:
    //   1 medicamento, 2 sucursal, 3 tipo, 4 tipo Despacho prohibido, 5 cantidad, 6 costo unitario (solo Compra), 7 motivo según tipo.
    //   Complementa las anotaciones de MovimientoInventarioDTO.CreateDTO (nivel 1).
    private void validarCampos(MovimientoInventarioDTO.CreateDTO dto) {
        if (dto.getMedicamentoId() == null) {
            throw new ValidationException("Debe seleccionar un medicamento.");
        }
        if (dto.getSucursalId() == null) {
            throw new ValidationException("Debe seleccionar una sucursal.");
        }
        if (dto.getTipoMovimiento() == null) {
            throw new ValidationException("Debe seleccionar el tipo de movimiento.");
        }
        // >>> VALIDACION RN-CU13-01: el tipo 6 (Despacho) es automático (lo genera CU-11) y no se acepta en creación manual.
        if (dto.getTipoMovimiento() == MovimientoInventario.DESPACHO) {
            throw new ValidationException("El tipo Despacho es generado automáticamente por el módulo de farmacia.");
        }
        // >>> VALIDACION RN-CU13-01: la cantidad debe ser un entero positivo.
        if (dto.getCantidad() == null || dto.getCantidad() <= 0) {
            throw new ValidationException("La cantidad debe ser un número entero positivo.");
        }

        short tipo = dto.getTipoMovimiento();

        // >>> VALIDACION RN-CU13-01: costo unitario obligatorio solo para Compra (> 0 y máximo 2 decimales).
        if (tipo == MovimientoInventario.COMPRA) {
            if (dto.getCostoUnitario() == null) {
                throw new ValidationException("El costo unitario es obligatorio para compras.");
            }
            if (dto.getCostoUnitario().doubleValue() <= 0) {
                throw new ValidationException("El costo unitario debe ser mayor a 0.");
            }
            if (dto.getCostoUnitario().scale() > 2) {
                throw new ValidationException("El costo unitario debe tener máximo 2 decimales.");
            }
        }

        // >>> VALIDACION RN-CU13-01: Motivo/Notas dinámico según tipo: obligatorio (10 a 500 caracteres) en Devolución, Reclamo, Ajuste+ y Ajuste-;
        // opcional en Compra; no aplica en Venta.
        boolean motivoObligatorio = tipo == MovimientoInventario.DEVOLUCION
                || tipo == MovimientoInventario.RECLAMO
                || tipo == MovimientoInventario.AJUSTE_POSITIVO
                || tipo == MovimientoInventario.AJUSTE_NEGATIVO;

        if (motivoObligatorio) {
            String motivo = dto.getMotivo();
            if (motivo == null || motivo.trim().length() < 10 || motivo.trim().length() > 500) {
                throw new ValidationException("El motivo debe contener entre 10 y 500 caracteres.");
            }
        }
    }

    // >>> Usado por FA02 / FB paso 8: tipos de salida = Venta (2), Reclamo (3) y Ajuste- (5).
    private boolean esTipoSalida(short tipo) {
        return tipo == MovimientoInventario.VENTA
                || tipo == MovimientoInventario.RECLAMO
                || tipo == MovimientoInventario.AJUSTE_NEGATIVO;
    }

    private MovimientoInventarioDTO.ResponseDTO toResponseDTO(MovimientoInventario m) {
        MovimientoInventarioDTO.ResponseDTO dto = new MovimientoInventarioDTO.ResponseDTO();
        dto.setId(m.getId());
        dto.setMedicamentoId(m.getMedicamento().getId());
        dto.setMedicamentoNombre(m.getMedicamento().getNombre());
        dto.setSucursalId(m.getSucursal().getId());
        dto.setSucursalNombre(m.getSucursal().getNombre());
        dto.setTipoMovimiento(m.getTipoMovimiento());
        dto.setTipoMovimientoNombre(NOMBRES_TIPO[m.getTipoMovimiento()]);
        dto.setCantidad(m.getCantidad());
        dto.setStockAnterior(m.getStockAnterior());
        dto.setStockNuevo(m.getStockNuevo());
        dto.setCostoUnitario(m.getCostoUnitario());
        dto.setNumeroReferencia(m.getNumeroReferencia());
        dto.setMotivo(m.getMotivo());
        dto.setUsuarioId(m.getUsuario().getId());
        dto.setUsuarioNombre(m.getUsuario().getNombreCompleto());
        dto.setActivo(m.getActivo());
        dto.setCreadoEn(m.getCreadoEn());
        return dto;
    }
}