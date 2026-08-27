package com.example.demo.controller;

import com.example.demo.dto.ExamenLaboratorioDTO;
import com.example.demo.dto.PageResponseDTO;
import com.example.demo.service.ExamenLaboratorioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static com.example.demo.config.CacheConfig.CACHE_EXAMENES_LABORATORIO;

// Catálogo "Exámenes de Laboratorio" (CU-15): CRUD paginado, restringido a
// Administrador General; alimenta el catálogo de exámenes usado en CU-08/CU-09.
@RestController
@RequestMapping("/api/examenes-laboratorio")
@RequiredArgsConstructor
public class ExamenLaboratorioController {

    private final ExamenLaboratorioService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR GENERAL','LABORATORISTA')")
    @Cacheable(CACHE_EXAMENES_LABORATORIO)
    public ResponseEntity<PageResponseDTO<ExamenLaboratorioDTO.ResponseDTO>> listar(
            @RequestParam(required = false) String filtro, Pageable pageable) {
        return ResponseEntity.ok(service.listar(filtro, pageable));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR GENERAL')")
    @CacheEvict(value = CACHE_EXAMENES_LABORATORIO, allEntries = true)
    public ResponseEntity<ExamenLaboratorioDTO.ResponseDTO> crear(@Valid @RequestBody ExamenLaboratorioDTO.CreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR GENERAL')")
    @CacheEvict(value = CACHE_EXAMENES_LABORATORIO, allEntries = true)
    public ResponseEntity<ExamenLaboratorioDTO.ResponseDTO> actualizar(@PathVariable Integer id,
                                                                       @Valid @RequestBody ExamenLaboratorioDTO.CreateDTO dto) {
        return ResponseEntity.ok(service.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR GENERAL')")
    @CacheEvict(value = CACHE_EXAMENES_LABORATORIO, allEntries = true)
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
