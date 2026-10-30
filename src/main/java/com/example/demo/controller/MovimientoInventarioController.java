package com.example.demo.controller;

import com.example.demo.dto.MovimientoInventarioDTO;
import com.example.demo.dto.PageResponseDTO;
import com.example.demo.security.AuthUsuario;
import com.example.demo.service.MovimientoInventarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// ============================================================================
// CU-15 BITÁCORA DE MOVIMIENTOS DE INVENTARIO (15_CU_Bitacora_Movimientos_Inventario.docx;
// en el código y en el RN consolidado figura como CU-13 / RN-CU13-xx). Listado paginado, registro manual de
// movimientos (Compra, Devolución, Venta, Reclamo, Ajuste+/-) y activar/desactivar registros.
// Cómo buscar en este archivo (Ctrl+F / Ctrl+Shift+F):
//   >>> INICIO FB     = donde empieza cada paso del Flujo Básico
//   >>> INICIO FA     = donde empieza cada Flujo Alterno
//   >>> VALIDACION    = donde se valida una regla de negocio (RN) o una precondición
// Las reglas de campos están en MovimientoInventarioService.validarCampos() y en las anotaciones de
// MovimientoInventarioDTO.CreateDTO. Este controlador solo dispara @Valid y controla roles (@PreAuthorize).
//
// FLUJO BÁSICO (FB)                                      ENDPOINT
//   Paso 1      menú 'Bitácora Inventario'                 front
//   Paso 2      listado + acciones por fila                GET   /api/movimientos-inventario | PATCH /{id}/estado
//   Pasos 3-5   'Nuevo Movimiento', formulario, tipo       front (panel de stock en tiempo real: otro endpoint)
//   Pasos 6-9   'Registrar Movimiento' -> valida -> stock  POST  /api/movimientos-inventario
// FLUJOS ALTERNOS (FA)
//   FA01 sin movimientos  -> front (listado vacío)       FA04 cancelar -> front
//   FA02 stock insuficiente (Venta/Reclamo/Ajuste-)  y  FA03 validación fallida -> POST (service y DTO)
//   FA05 alerta preventiva de stock mínimo -> NO existe en backend (es del front)
// Acceso: @PreAuthorize (Admin General, Administrador de Sede, Farmacéutico). SecurityConfig usa '/api/movimientosinventario/**'
// (sin guion), que NO coincide con esta ruta; por eso el control real de rol es el @PreAuthorize.
// ============================================================================
@RestController
@RequestMapping("/api/movimientos-inventario")
@RequiredArgsConstructor
public class MovimientoInventarioController {

    private final MovimientoInventarioService service;

    // >>> INICIO FB - PASO 2: listado paginado (solo el filtro de sede + Pageable; los filtros por columna del CU no están en backend).
    // Administrador de Sede y Farmacéutico solo ven/registran movimientos de su propia sucursal.
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR GENERAL','ADMINISTRADOR','FARMACÉUTICO')")
    public ResponseEntity<PageResponseDTO<MovimientoInventarioDTO.ResponseDTO>> listar(
            Pageable pageable, @AuthenticationPrincipal AuthUsuario usuario) {
        return ResponseEntity.ok(service.listar(pageable, usuario.sucursalScopeOrNull()));
    }

    // >>> INICIO FB - PASOS 6 a 9: 'Registrar Movimiento'.
    // >>> VALIDACION FA03 (nivel 1): @Valid ejecuta las anotaciones de MovimientoInventarioDTO.CreateDTO; (nivel 2) service.registrar() -> validarCampos().
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR GENERAL','ADMINISTRADOR','FARMACÉUTICO')")
    public ResponseEntity<MovimientoInventarioDTO.ResponseDTO> registrar(
            @Valid @RequestBody MovimientoInventarioDTO.CreateDTO dto,
            @AuthenticationPrincipal AuthUsuario usuarioActual) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.registrar(dto, usuarioActual.getId(), usuarioActual.sucursalScopeOrNull()));
    }

    // >>> INICIO FB - PASO 2 (acción por fila 'Desactivar/Activar'): cambia solo el flag 'activo' del registro (no revierte el stock).
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR GENERAL','ADMINISTRADOR','FARMACÉUTICO')")
    public ResponseEntity<MovimientoInventarioDTO.ResponseDTO> cambiarEstado(
            @PathVariable Integer id, @RequestParam boolean activo, @AuthenticationPrincipal AuthUsuario usuario) {
        return ResponseEntity.ok(service.cambiarEstado(id, activo, usuario.sucursalScopeOrNull()));
    }
}
