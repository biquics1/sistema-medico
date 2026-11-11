package com.example.demo.controller;

import com.example.demo.dto.PageResponseDTO;
import com.example.demo.dto.TarjetaMarcaDTO;
import com.example.demo.service.TarjetaMarcaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// Catálogo "Marcas de Tarjeta" (CU-14) con el conteo de usos en pagos (CU-04/CU-06).
// Sin caché: el contador debe verse siempre actualizado.
@RestController
@RequestMapping("/api/tarjetas")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMINISTRADOR GENERAL')")
public class TarjetaMarcaController {

    private final TarjetaMarcaService service;
    private final com.example.demo.service.TarjetaRegistradaService registradaService;

    @GetMapping("/admin")
    public ResponseEntity<PageResponseDTO<TarjetaMarcaDTO.ResponseDTO>> listar(
            @RequestParam(required = false) String filtro, Pageable pageable) {
        return ResponseEntity.ok(service.listar(filtro, pageable));
    }

    // Historial de tarjetas distintas usadas en pagos en línea (CU-04), solo lectura.
    @GetMapping("/registradas")
    public ResponseEntity<PageResponseDTO<TarjetaMarcaDTO.RegistradaDTO>> registradas(
            @RequestParam(required = false) String filtro, Pageable pageable) {
        return ResponseEntity.ok(registradaService.listar(filtro, pageable));
    }

    @PostMapping
    public ResponseEntity<TarjetaMarcaDTO.ResponseDTO> crear(@Valid @RequestBody TarjetaMarcaDTO.CreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TarjetaMarcaDTO.ResponseDTO> actualizar(@PathVariable Integer id,
                                                                  @Valid @RequestBody TarjetaMarcaDTO.CreateDTO dto) {
        return ResponseEntity.ok(service.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
