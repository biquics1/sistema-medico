package com.example.demo.controller;

import com.example.demo.dto.EstadoCitaDTO;
import com.example.demo.dto.PageResponseDTO;
import com.example.demo.service.EstadoCitaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static com.example.demo.config.CacheConfig.CACHE_ESTADOS_CITA;

@RestController
@RequestMapping("/api/estados-cita")
@RequiredArgsConstructor
public class EstadoCitaController {

    private final EstadoCitaService service;

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR GENERAL')")
    @Cacheable(CACHE_ESTADOS_CITA)
    public ResponseEntity<PageResponseDTO<EstadoCitaDTO.ResponseDTO>> listar(
            @RequestParam(required = false) String filtro, Pageable pageable) {
        return ResponseEntity.ok(service.listar(filtro, pageable));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR GENERAL')")
    @CacheEvict(value = CACHE_ESTADOS_CITA, allEntries = true)
    public ResponseEntity<EstadoCitaDTO.ResponseDTO> crear(@Valid @RequestBody EstadoCitaDTO.CreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR GENERAL')")
    @CacheEvict(value = CACHE_ESTADOS_CITA, allEntries = true)
    public ResponseEntity<EstadoCitaDTO.ResponseDTO> actualizar(@PathVariable Integer id,
                                                                 @Valid @RequestBody EstadoCitaDTO.CreateDTO dto) {
        return ResponseEntity.ok(service.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR GENERAL')")
    @CacheEvict(value = CACHE_ESTADOS_CITA, allEntries = true)
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
