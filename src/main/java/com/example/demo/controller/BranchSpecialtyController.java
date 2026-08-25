package com.example.demo.controller;

import com.example.demo.dto.BranchSpecialtyCreateDTO;
import com.example.demo.dto.BranchSpecialtyResponseDTO;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.ValidationException;
import com.example.demo.modelo.BranchSpecialty;
import com.example.demo.modelo.Especialidad;
import com.example.demo.modelo.Sucursal;
import com.example.demo.repository.BranchSpecialtyRepository;
import com.example.demo.repository.EspecialidadRepository;
import com.example.demo.repository.SucursalRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static com.example.demo.config.CacheConfig.CACHE_BRANCH_SPECIALTY;

@RestController
@RequestMapping("/api/branch-specialty")
@RequiredArgsConstructor
public class BranchSpecialtyController {

    private final BranchSpecialtyRepository branchSpecialtyRepository;
    private final SucursalRepository sucursalRepository;
    private final EspecialidadRepository especialidadRepository;

    // Paso 2-3 FB: listado con filtro por ID y paginación. FA01 lo cubre TableServer en el front (tabla vacía).
    // CACHEADO: alimenta el paso 2 del wizard de citas (CU-03) para filtrar
    // especialidades disponibles por sede, así que se consulta muy seguido.
    @GetMapping
    @Cacheable(CACHE_BRANCH_SPECIALTY)
    public List<BranchSpecialtyResponseDTO> listar() {
        return branchSpecialtyRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    // Paso 6-8 FB: asignar especialidad a sede
    @PostMapping
    @CacheEvict(value = CACHE_BRANCH_SPECIALTY, allEntries = true)
    public ResponseEntity<Map<String, String>> asignar(@Valid @RequestBody BranchSpecialtyCreateDTO dto) {

        Sucursal sucursal = sucursalRepository.findById(dto.sucursalId())
                .orElseThrow(() -> new ResourceNotFoundException("La sede indicada no existe."));

        Especialidad especialidad = especialidadRepository.findById(dto.especialidadId())
                .orElseThrow(() -> new ResourceNotFoundException("La especialidad indicada no existe."));

        // FA05: validar duplicado antes de guardar
        if (branchSpecialtyRepository.existsBySucursal_IdAndEspecialidad_Id(dto.sucursalId(), dto.especialidadId())) {
            throw new ValidationException("La asignación ya existe.");
        }

        BranchSpecialty entidad = new BranchSpecialty();
        entidad.setSucursal(sucursal);
        entidad.setEspecialidad(especialidad);
        entidad.setEstado((short) 1); // estado se asigna automáticamente como activo [RN-CU12-01]

        branchSpecialtyRepository.save(entidad);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("mensaje", "Especialidad asignada a la sede correctamente"));
    }

    // FA02: eliminar asignación
    @DeleteMapping("/{id}")
    @CacheEvict(value = CACHE_BRANCH_SPECIALTY, allEntries = true)
    public ResponseEntity<Map<String, String>> eliminar(@PathVariable Integer id) {
        BranchSpecialty entidad = branchSpecialtyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("La asignación indicada no existe."));

        branchSpecialtyRepository.delete(entidad);

        return ResponseEntity.ok(Map.of("mensaje", "Asignación eliminada correctamente"));
    }

    private BranchSpecialtyResponseDTO toDTO(BranchSpecialty bs) {
        return new BranchSpecialtyResponseDTO(
                bs.getId(),
                bs.getSucursal().getId(),
                bs.getSucursal().getNombre(),
                bs.getEspecialidad().getId(),
                bs.getEspecialidad().getNombre(),
                bs.getEstado()
        );
    }
}
