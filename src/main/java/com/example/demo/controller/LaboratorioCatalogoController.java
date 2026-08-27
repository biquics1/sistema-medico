package com.example.demo.controller;

import com.example.demo.dto.CatalogoSimpleDTO;
import com.example.demo.dto.LaboratorioCatalogoDTO;
import com.example.demo.dto.PageResponseDTO;
import com.example.demo.repository.LaboratorioRepository;
import com.example.demo.service.LaboratorioCatalogoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.example.demo.config.CacheConfig.CACHE_LABORATORIOS;

// NOTA: usa "/api/laboratorios" (plural) porque "/api/laboratorio" (singular) ya
// está tomado por el LaboratorioController de órdenes de laboratorio (CU-09).
// Este controller es el catálogo (la entidad "Laboratorio Clínico Central", etc.),
// no las órdenes.
@RestController
@RequestMapping("/api/laboratorios")
@RequiredArgsConstructor
public class LaboratorioCatalogoController {

    private final LaboratorioRepository laboratorioRepository;
    private final LaboratorioCatalogoService laboratorioCatalogoService;

    // Listado plano para el dropdown de "Laboratorio" al crear un Examen de Laboratorio.
    // CACHEADO: catálogo de solo lectura, consultado en cada alta/edición de examen.
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR GENERAL','LABORATORISTA')")
    @Cacheable(CACHE_LABORATORIOS)
    public List<CatalogoSimpleDTO> listar() {
        return laboratorioRepository.findAll().stream()
                .filter(l -> l.getEstado() != null && l.getEstado() == 1)
                .map(l -> new CatalogoSimpleDTO(l.getId(), l.getNombre()))
                .toList();
    }

    // Listado paginado/filtrable para la pantalla de administración de catálogos.
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMINISTRADOR GENERAL')")
    @Cacheable(CACHE_LABORATORIOS)
    public ResponseEntity<PageResponseDTO<LaboratorioCatalogoDTO.ResponseDTO>> listarAdmin(
            @RequestParam(required = false) String filtro, Pageable pageable) {
        return ResponseEntity.ok(laboratorioCatalogoService.listar(filtro, pageable));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR GENERAL')")
    @CacheEvict(value = CACHE_LABORATORIOS, allEntries = true)
    public ResponseEntity<LaboratorioCatalogoDTO.ResponseDTO> crear(@Valid @RequestBody LaboratorioCatalogoDTO.CreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(laboratorioCatalogoService.crear(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR GENERAL')")
    @CacheEvict(value = CACHE_LABORATORIOS, allEntries = true)
    public ResponseEntity<LaboratorioCatalogoDTO.ResponseDTO> actualizar(@PathVariable Integer id,
                                                                         @Valid @RequestBody LaboratorioCatalogoDTO.CreateDTO dto) {
        return ResponseEntity.ok(laboratorioCatalogoService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR GENERAL')")
    @CacheEvict(value = CACHE_LABORATORIOS, allEntries = true)
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        laboratorioCatalogoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
