package com.example.demo.controller;

import com.example.demo.dto.CatalogoSimpleDTO;
import com.example.demo.dto.PageResponseDTO;
import com.example.demo.dto.RolDTO;
import com.example.demo.repository.RolRepository;
import com.example.demo.service.RolService;
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

import static com.example.demo.config.CacheConfig.CACHE_ROLES;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RolController {

    private final RolRepository rolRepository;
    private final RolService rolService;

    // Listado plano usado por selects/dropdowns en toda la app (SIN TOCAR la lógica).
    // CACHEADO: es el catálogo que CU-00/01/03/04/07 esperan "precargado en caché".
    @GetMapping
    @Cacheable(CACHE_ROLES)
    public List<CatalogoSimpleDTO> listar() {
        return rolRepository.findAll().stream()
                .filter(r -> r.getEstado() != null && r.getEstado() == 1)
                .map(r -> new CatalogoSimpleDTO(r.getId(), r.getNombre()))
                .toList();
    }

    // NUEVO: listado paginado/filtrable para la pantalla de administración de catálogos.
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMINISTRADOR GENERAL')")
    @Cacheable(CACHE_ROLES)
    public ResponseEntity<PageResponseDTO<RolDTO.ResponseDTO>> listarAdmin(
            @RequestParam(required = false) String filtro, Pageable pageable) {
        return ResponseEntity.ok(rolService.listar(filtro, pageable));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR GENERAL')")
    @CacheEvict(value = CACHE_ROLES, allEntries = true)
    public ResponseEntity<RolDTO.ResponseDTO> crear(@Valid @RequestBody RolDTO.CreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rolService.crear(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR GENERAL')")
    @CacheEvict(value = CACHE_ROLES, allEntries = true)
    public ResponseEntity<RolDTO.ResponseDTO> actualizar(@PathVariable Integer id,
                                                           @Valid @RequestBody RolDTO.CreateDTO dto) {
        return ResponseEntity.ok(rolService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR GENERAL')")
    @CacheEvict(value = CACHE_ROLES, allEntries = true)
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        rolService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
