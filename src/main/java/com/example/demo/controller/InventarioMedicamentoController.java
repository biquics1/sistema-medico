package com.example.demo.controller;

import com.example.demo.dto.InventarioMedicamentoDTO;
import com.example.demo.dto.PageResponseDTO;
import com.example.demo.security.AuthUsuario;
import com.example.demo.service.InventarioMedicamentoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/inventario-medicamentos")
@RequiredArgsConstructor
public class InventarioMedicamentoController {

    private final InventarioMedicamentoService service;

    // Administrador de Sede y Farmacéutico solo ven/gestionan el inventario
    // de su propia sucursal
    // (filtrado real vía AuthUsuario.sucursalScopeOrNull() dentro del service).
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR GENERAL','ADMINISTRADOR','FARMACÉUTICO')")
    public ResponseEntity<PageResponseDTO<InventarioMedicamentoDTO.ResponseDTO>> listar(
            Pageable pageable, @AuthenticationPrincipal AuthUsuario usuario) {
        return ResponseEntity.ok(service.listar(pageable, usuario.sucursalScopeOrNull()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR GENERAL','ADMINISTRADOR','FARMACÉUTICO')")
    public ResponseEntity<InventarioMedicamentoDTO.ResponseDTO> crear(
            @Valid @RequestBody InventarioMedicamentoDTO.CreateDTO dto, @AuthenticationPrincipal AuthUsuario usuario) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(dto, usuario.sucursalScopeOrNull()));
    }

    // LowStockAlert [RN-CU10-03]
    @GetMapping("/alertas-stock-bajo")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR GENERAL','ADMINISTRADOR','FARMACÉUTICO')")
    public ResponseEntity<List<InventarioMedicamentoDTO.LowStockAlertDTO>> alertasStockBajo(
            @AuthenticationPrincipal AuthUsuario usuario) {
        return ResponseEntity.ok(service.alertasStockBajo(usuario.sucursalScopeOrNull()));
    }

    // MedicineInventorySummary - resumen mensual [RN-CU13-03]
    @GetMapping("/resumen-mensual")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR GENERAL','ADMINISTRADOR','FARMACÉUTICO')")
    public ResponseEntity<InventarioMedicamentoDTO.SummaryDTO> resumenMensual(
            @RequestParam Integer medicamentoId,
            @RequestParam Integer sucursalId,
            @RequestParam(required = false) String mes,
            @AuthenticationPrincipal AuthUsuario usuario) {
        YearMonth yearMonth = (mes != null && !mes.isBlank()) ? YearMonth.parse(mes) : YearMonth.now();
        return ResponseEntity.ok(service.resumenMensual(medicamentoId, sucursalId, yearMonth, usuario.sucursalScopeOrNull()));
    }
}
