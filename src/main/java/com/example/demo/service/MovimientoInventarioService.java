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

    public PageResponseDTO<MovimientoInventarioDTO.ResponseDTO> listar(Pageable pageable, Integer sucursalScope) {
        Page<MovimientoInventario> page = repository.findAll(
                MovimientoInventarioSpecifications.sucursalIdEs(sucursalScope), pageable);

        List<MovimientoInventarioDTO.ResponseDTO> contenido = page.getContent().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());

        return new PageResponseDTO<>(contenido, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    @Transactional
    public MovimientoInventarioDTO.ResponseDTO registrar(MovimientoInventarioDTO.CreateDTO dto, Integer usuarioIdActual,
                                                         Integer sucursalScope) {
        auditoriaContexto.aplicar();
        validarCampos(dto);
        if (sucursalScope != null && !sucursalScope.equals(dto.getSucursalId())) {
            throw new ValidationException("No tiene permiso para registrar movimientos en otra sucursal.");
        }

        Medicamento medicamento = medicamentoRepository.findById(dto.getMedicamentoId())
                .orElseThrow(() -> new ValidationException("Debe seleccionar un medicamento."));
        Sucursal sucursal = sucursalRepository.findById(dto.getSucursalId())
                .orElseThrow(() -> new ValidationException("Debe seleccionar una sucursal."));
        Usuario usuario = usuarioRepository.findById(usuarioIdActual)
                .orElseThrow(() -> new ValidationException("Usuario no válido."));

        InventarioMedicamento inventario = inventarioRepository
                .findByMedicamentoIdAndSucursalId(dto.getMedicamentoId(), dto.getSucursalId())
                .orElseGet(() -> {
                    InventarioMedicamento nuevo = new InventarioMedicamento();
                    nuevo.setMedicamento(medicamento);
                    nuevo.setSucursal(sucursal);
                    nuevo.setStockActual(0);
                    return inventarioRepository.save(nuevo);
                });

        int stockAnterior = inventario.getStockActual();
        int stockNuevo;
        boolean esSalida = esTipoSalida(dto.getTipoMovimiento());

        if (esSalida) {
            // RN-CU13-02: Venta, Reclamo y Ajuste- no pueden exceder el stock actual
            if (dto.getCantidad() > stockAnterior) {
                throw new ValidationException("Stock insuficiente. Stock actual: " + stockAnterior
                        + ". No se puede registrar una salida de " + dto.getCantidad() + " unidades.");
            }
            stockNuevo = stockAnterior - dto.getCantidad();
        } else {
            stockNuevo = stockAnterior + dto.getCantidad();
        }

        inventario.setStockActual(stockNuevo);
        try {
            inventarioRepository.save(inventario);
        } catch (OptimisticLockingFailureException ex) {
            throw new ValidationException("El inventario fue modificado por otra operación. Intente nuevamente.");
        }

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

        return toResponseDTO(repository.save(movimiento));
    }

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
        if (dto.getTipoMovimiento() == MovimientoInventario.DESPACHO) {
            throw new ValidationException("El tipo Despacho es generado automáticamente por el módulo de farmacia.");
        }
        if (dto.getCantidad() == null || dto.getCantidad() <= 0) {
            throw new ValidationException("La cantidad debe ser un número entero positivo.");
        }

        short tipo = dto.getTipoMovimiento();

        // Costo unitario obligatorio solo para Compra
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

        // Motivo/Notas dinámico según tipo
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