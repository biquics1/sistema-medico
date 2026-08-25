package com.example.demo.controller;

import com.example.demo.dto.CatalogoSimpleDTO;
import com.example.demo.dto.EspecialidadDTO;
import com.example.demo.dto.PageResponseDTO;
import com.example.demo.repository.EspecialidadRepository;
import com.example.demo.service.EspecialidadService;
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

import static com.example.demo.config.CacheConfig.CACHE_ESPECIALIDADES;

@RestController
@RequestMapping("/api/especialidades")
@RequiredArgsConstructor
public class EspecialidadController {

    private final EspecialidadRepository especialidadRepository;
    private final EspecialidadService especialidadService;

    // Listado plano usado por selects/dropdowns en toda la app (SIN TOCAR la lógica).
    // CACHEADO: es el catálogo que CU-00/01/03/04/07 esperan "precargado en caché".
    @GetMapping
    @Cacheable(CACHE_ESPECIALIDADES)
    public List<CatalogoSimpleDTO> listar() {
        return especialidadRepository.findAll().stream()
                .filter(e -> e.getEstado() != null && e.getEstado() == 1)
                .map(e -> new CatalogoSimpleDTO(e.getId(), e.getNombre()))
                .toList();
    }

    // NUEVO: listado paginado/filtrable para la pantalla de administración de catálogos.
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMINISTRADOR GENERAL')")
    @Cacheable(CACHE_ESPECIALIDADES)
    public ResponseEntity<PageResponseDTO<EspecialidadDTO.ResponseDTO>> listarAdmin(
            @RequestParam(required = false) String filtro, Pageable pageable) {
        return ResponseEntity.ok(especialidadService.listar(filtro, pageable));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR GENERAL')")
    @CacheEvict(value = CACHE_ESPECIALIDADES, allEntries = true)
    public ResponseEntity<EspecialidadDTO.ResponseDTO> crear(@Valid @RequestBody EspecialidadDTO.CreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(especialidadService.crear(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR GENERAL')")
    @CacheEvict(value = CACHE_ESPECIALIDADES, allEntries = true)
    public ResponseEntity<EspecialidadDTO.ResponseDTO> actualizar(@PathVariable Integer id,
                                                                    @Valid @RequestBody EspecialidadDTO.CreateDTO dto) {
        return ResponseEntity.ok(especialidadService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR GENERAL')")
    @CacheEvict(value = CACHE_ESPECIALIDADES, allEntries = true)
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        especialidadService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
