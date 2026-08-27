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

// Bitácora de Movimientos de Inventario (CU-13): listado paginado, registro
// manual de movimientos (Compra, Devolución, Venta, Reclamo, Ajuste+/-) y
// activar/desactivar registros.
@RestController
@RequestMapping("/api/movimientos-inventario")
@RequiredArgsConstructor
public class MovimientoInventarioController {

    private final MovimientoInventarioService service;

    // Administrador de Sede y Farmacéutico solo ven/registran movimientos de su propia sucursal.
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR GENERAL','ADMINISTRADOR','FARMACÉUTICO')")
    public ResponseEntity<PageResponseDTO<MovimientoInventarioDTO.ResponseDTO>> listar(
            Pageable pageable, @AuthenticationPrincipal AuthUsuario usuario) {
        return ResponseEntity.ok(service.listar(pageable, usuario.sucursalScopeOrNull()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR GENERAL','ADMINISTRADOR','FARMACÉUTICO')")
    public ResponseEntity<MovimientoInventarioDTO.ResponseDTO> registrar(
            @Valid @RequestBody MovimientoInventarioDTO.CreateDTO dto,
            @AuthenticationPrincipal AuthUsuario usuarioActual) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.registrar(dto, usuarioActual.getId(), usuarioActual.sucursalScopeOrNull()));
    }

    // Desactivar/Activar (toggle de estado del registro)
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR GENERAL','ADMINISTRADOR','FARMACÉUTICO')")
    public ResponseEntity<MovimientoInventarioDTO.ResponseDTO> cambiarEstado(
            @PathVariable Integer id, @RequestParam boolean activo, @AuthenticationPrincipal AuthUsuario usuario) {
        return ResponseEntity.ok(service.cambiarEstado(id, activo, usuario.sucursalScopeOrNull()));
    }
}
