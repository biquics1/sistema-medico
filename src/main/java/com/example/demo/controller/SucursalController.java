package com.example.demo.controller;

import com.example.demo.dto.CatalogoSimpleDTO;
import com.example.demo.dto.PageResponseDTO;
import com.example.demo.dto.SucursalDTO;
import com.example.demo.repository.SucursalRepository;
import com.example.demo.service.SucursalService;
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

import static com.example.demo.config.CacheConfig.CACHE_SUCURSALES;

@RestController
@RequestMapping("/api/sucursales")
@RequiredArgsConstructor
public class SucursalController {

    private final SucursalRepository sucursalRepository;
    private final SucursalService sucursalService;

    // Listado plano usado por selects/dropdowns en toda la app (SIN TOCAR la lógica).
    // CACHEADO: es el catálogo que CU-00/01/03/04/07 esperan "precargado en caché".
    @GetMapping
    @Cacheable(CACHE_SUCURSALES)
    public List<CatalogoSimpleDTO> listar() {
        return sucursalRepository.findAll().stream()
                .filter(s -> s.getEstado() != null && s.getEstado() == 1)
                .map(s -> new CatalogoSimpleDTO(s.getId(), s.getNombre()))
                .toList();
    }

    // NUEVO: listado paginado/filtrable para la pantalla de administración de catálogos.
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMINISTRADOR GENERAL')")
    @Cacheable(CACHE_SUCURSALES)
    public ResponseEntity<PageResponseDTO<SucursalDTO.ResponseDTO>> listarAdmin(
            @RequestParam(required = false) String filtro, Pageable pageable) {
        return ResponseEntity.ok(sucursalService.listar(filtro, pageable));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR GENERAL')")
    @CacheEvict(value = CACHE_SUCURSALES, allEntries = true)
    public ResponseEntity<SucursalDTO.ResponseDTO> crear(@Valid @RequestBody SucursalDTO.CreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sucursalService.crear(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR GENERAL')")
    @CacheEvict(value = CACHE_SUCURSALES, allEntries = true)
    public ResponseEntity<SucursalDTO.ResponseDTO> actualizar(@PathVariable Integer id,
                                                                @Valid @RequestBody SucursalDTO.CreateDTO dto) {
        return ResponseEntity.ok(sucursalService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR GENERAL')")
    @CacheEvict(value = CACHE_SUCURSALES, allEntries = true)
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        sucursalService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
