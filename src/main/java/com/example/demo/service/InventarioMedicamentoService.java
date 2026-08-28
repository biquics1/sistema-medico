package com.example.demo.service;

import com.example.demo.dto.InventarioMedicamentoDTO;
import com.example.demo.dto.PageResponseDTO;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.ValidationException;
import com.example.demo.modelo.InventarioMedicamento;
import com.example.demo.modelo.Medicamento;
import com.example.demo.modelo.MovimientoInventario;
import com.example.demo.modelo.Sucursal;
import com.example.demo.repository.InventarioMedicamentoRepository;
import com.example.demo.repository.InventarioMedicamentoSpecifications;
import com.example.demo.repository.MedicamentoRepository;
import com.example.demo.repository.MovimientoInventarioRepository;
import com.example.demo.repository.SucursalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventarioMedicamentoService {
    private final AuditoriaContexto auditoriaContexto;

    private final InventarioMedicamentoRepository repository;
    private final MedicamentoRepository medicamentoRepository;
    private final SucursalRepository sucursalRepository;
    private final MovimientoInventarioRepository movimientoRepository;

    public PageResponseDTO<InventarioMedicamentoDTO.ResponseDTO> listar(Pageable pageable, Integer sucursalScope) {
        Page<InventarioMedicamento> page = repository.findAll(
                InventarioMedicamentoSpecifications.sucursalIdEs(sucursalScope), pageable);

        List<InventarioMedicamentoDTO.ResponseDTO> contenido = page.getContent().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());

        return new PageResponseDTO<>(contenido, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    @Transactional
    public InventarioMedicamentoDTO.ResponseDTO crear(InventarioMedicamentoDTO.CreateDTO dto, Integer sucursalScope) {
        auditoriaContexto.aplicar();
        if (sucursalScope != null && !sucursalScope.equals(dto.getSucursalId())) {
            throw new ValidationException("No tiene permiso para crear inventario en otra sucursal.");
        }
        repository.findByMedicamentoIdAndSucursalId(dto.getMedicamentoId(), dto.getSucursalId())
                .ifPresent(i -> {
                    throw new ValidationException("Ya existe un registro de inventario para este medicamento en esta sucursal.");
                });
        Medicamento medicamento = medicamentoRepository.findById(dto.getMedicamentoId())
                .orElseThrow(() -> new ValidationException("Debe seleccionar un medicamento."));
        Sucursal sucursal = sucursalRepository.findById(dto.getSucursalId())
                .orElseThrow(() -> new ValidationException("Debe seleccionar una sucursal."));

        InventarioMedicamento entidad = new InventarioMedicamento();
        entidad.setMedicamento(medicamento);
        entidad.setSucursal(sucursal);
        entidad.setStockActual(dto.getStockActual());
        return toResponseDTO(repository.save(entidad));
    }

    // Alerta de stock bajo [RN-CU10-03]
    public List<InventarioMedicamentoDTO.LowStockAlertDTO> alertasStockBajo(Integer sucursalScope) {
        return repository.findAllStockBajo().stream()
                .filter(i -> sucursalScope == null || sucursalScope.equals(i.getSucursal().getId()))
                .map(i -> {
                    InventarioMedicamentoDTO.LowStockAlertDTO dto = new InventarioMedicamentoDTO.LowStockAlertDTO();
                    dto.setMedicamentoId(i.getMedicamento().getId());
                    dto.setMedicamentoNombre(i.getMedicamento().getNombre());
                    dto.setSucursalId(i.getSucursal().getId());
                    dto.setSucursalNombre(i.getSucursal().getNombre());
                    dto.setStockActual(i.getStockActual());
                    dto.setStockMinimo(i.getMedicamento().getStockMinimo());
                    dto.setMensaje("El stock del medicamento " + i.getMedicamento().getNombre()
                            + " ha alcanzado el nivel mínimo (" + i.getStockActual() + " unidades restantes). "
                            + "Se requiere reorden.");
                    return dto;
                }).collect(Collectors.toList());
    }

    // Resumen mensual de movimientos para un medicamento/sucursal [RN-CU13-03]
    public InventarioMedicamentoDTO.SummaryDTO resumenMensual(Integer medicamentoId, Integer sucursalId, YearMonth mes,
                                                              Integer sucursalScope) {
        if (sucursalScope != null && !sucursalScope.equals(sucursalId)) {
            throw new ValidationException("No tiene permiso para consultar el resumen de otra sucursal.");
        }
        InventarioMedicamento inventario = repository.findByMedicamentoIdAndSucursalId(medicamentoId, sucursalId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe inventario para este medicamento en esta sucursal."));

        LocalDateTime inicio = mes.atDay(1).atStartOfDay();
        LocalDateTime fin = mes.atEndOfMonth().atTime(23, 59, 59);
        List<MovimientoInventario> movimientos = movimientoRepository.findResumenMensual(medicamentoId, sucursalId, inicio, fin);

        long entradas = movimientos.stream()
                .filter(m -> esEntrada(m.getTipoMovimiento()))
                .mapToLong(MovimientoInventario::getCantidad).sum();
        long salidas = movimientos.stream()
                .filter(m -> !esEntrada(m.getTipoMovimiento()))
                .mapToLong(MovimientoInventario::getCantidad).sum();

        InventarioMedicamentoDTO.SummaryDTO dto = new InventarioMedicamentoDTO.SummaryDTO();
        dto.setMedicamentoId(medicamentoId);
        dto.setMedicamentoNombre(inventario.getMedicamento().getNombre());
        dto.setSucursalId(sucursalId);
        dto.setSucursalNombre(inventario.getSucursal().getNombre());
        dto.setMesAnio(mes.toString());
        dto.setTotalEntradas(entradas);
        dto.setTotalSalidas(salidas);
        dto.setStockActual(inventario.getStockActual());
        return dto;
    }

    private boolean esEntrada(short tipo) {
        return tipo == MovimientoInventario.COMPRA || tipo == MovimientoInventario.DEVOLUCION
                || tipo == MovimientoInventario.AJUSTE_POSITIVO;
    }

    private InventarioMedicamentoDTO.ResponseDTO toResponseDTO(InventarioMedicamento i) {
        InventarioMedicamentoDTO.ResponseDTO dto = new InventarioMedicamentoDTO.ResponseDTO();
        dto.setId(i.getId());
        dto.setMedicamentoId(i.getMedicamento().getId());
        dto.setMedicamentoNombre(i.getMedicamento().getNombre());
        dto.setSucursalId(i.getSucursal().getId());
        dto.setSucursalNombre(i.getSucursal().getNombre());
        dto.setStockActual(i.getStockActual());
        Integer stockMinimo = i.getMedicamento().getStockMinimo();
        dto.setStockMinimo(stockMinimo);
        dto.setStockBajo(stockMinimo != null && i.getStockActual() <= stockMinimo);
        dto.setVersion(i.getVersion());
        return dto;
    }
}