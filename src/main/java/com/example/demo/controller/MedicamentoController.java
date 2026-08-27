package com.example.demo.controller;

import com.example.demo.dto.CatalogoSimpleDTO;
import com.example.demo.dto.FarmaciaDTOs.CrearMedicamentoRequestDTO;
import com.example.demo.dto.FarmaciaDTOs.MedicamentoCatalogoDTO;
import com.example.demo.repository.MedicamentoRepository;
import com.example.demo.service.FarmaciaService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.example.demo.config.CacheConfig.CACHE_MEDICAMENTOS;

// Catálogo "Medicamentos" (CU-15): listado simple cacheado para dropdowns
// (ej. "medicamento sustituto" en Farmacia) y alta de nuevos medicamentos.
@RestController
@RequestMapping("/api/medicamentos")
@RequiredArgsConstructor
public class MedicamentoController {

    private final MedicamentoRepository medicamentoRepository;
    private final FarmaciaService farmaciaService;

    // Usado por el módulo de Farmacia (CU-11) para el select de "medicamento sustituto"
    // CACHEADO: catálogo de solo lectura, consultado muy seguido en despacho.
    @GetMapping
    @Cacheable(CACHE_MEDICAMENTOS)
    public List<CatalogoSimpleDTO> listar() {
        return medicamentoRepository.findByEstado((short) 1).stream()
                .map(m -> new CatalogoSimpleDTO(m.getId(), m.getNombre()))
                .toList();
    }

    // NUEVO: alta de medicamentos al catálogo, desde la pantalla de Farmacia (Administrador)
    @PostMapping
    @CacheEvict(value = CACHE_MEDICAMENTOS, allEntries = true)
    public MedicamentoCatalogoDTO crear(@RequestBody CrearMedicamentoRequestDTO request) {
        return farmaciaService.crearMedicamento(request);
    }
}
