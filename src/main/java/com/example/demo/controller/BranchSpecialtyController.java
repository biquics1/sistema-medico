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

// ============================================================================
// CU-13 CONFIGURACIÓN DE SEDES Y ESPECIALIDADES (13_CU_Configuracion_Sedes_Especialidades.docx;
// en el código y en el RN consolidado figura como CU-12 / RN-CU12-01). Catálogo "Sucursal-Especialidad":
// asigna qué especialidades están disponibles en cada sede. Solo permite crear y eliminar (sin edición),
// y alimenta el paso 2 del wizard de citas (CU-03).
// Cómo buscar en este archivo (Ctrl+F / Ctrl+Shift+F):
//   >>> INICIO FB     = donde empieza cada paso del Flujo Básico
//   >>> INICIO FA     = donde empieza cada Flujo Alterno
//   >>> VALIDACION    = donde se valida una regla de negocio (RN) o una precondición
//
// FLUJO BÁSICO (FB)                                    DÓNDE
//   Pasos 1-2   Dashboard -> 'Especialidades por Sede'    front
//   Paso 3      listado (ID, Sede, Especialidad, Estado)  GET    listar()
//   Pasos 4-6   'Asignar Especialidad' + formulario       front
//   Pasos 7-9   'Asignar' -> valida duplicado -> guarda   POST   asignar()
// FLUJOS ALTERNOS (FA)
//   FA01 sin asignaciones   -> listar() devuelve []; el mensaje de tabla vacía es del front (TableServer)
//   FA02 eliminar           -> DELETE eliminar()  (el diálogo '¿Confirmar eliminación?' es del front)
//   FA03 campos vacíos      -> @Valid + BranchSpecialtyCreateDTO (@NotNull) + 'existe' en asignar()
//   FA04 cancelar           -> solo front (no hay endpoint)
//   FA05 duplicado          -> asignar(), 'existsBySucursal_IdAndEspecialidad_Id' (+ UNIQUE en BD)
// Acceso (SecurityConfig): solo Administrador General.
// PENDIENTE vs CU: este controller no llama a auditoriaContexto.aplicar() (otros servicios sí) y listar() no filtra por ID ni pagina en backend.
// ============================================================================
@RestController
@RequestMapping("/api/branch-specialty")
@RequiredArgsConstructor
public class BranchSpecialtyController {

    private final BranchSpecialtyRepository branchSpecialtyRepository;
    private final SucursalRepository sucursalRepository;
    private final EspecialidadRepository especialidadRepository;

    // >>> INICIO FB - PASOS 2 y 3: listado (el filtro por ID y la paginación los hace el front con TableServer).
    // >>> INICIO FA01 (sin asignaciones): aquí solo se devuelve lista vacía; el mensaje lo muestra el front (tabla vacía).
    // CACHEADO: alimenta el paso 2 del wizard de citas (CU-03) para filtrar
    // especialidades disponibles por sede, así que se consulta muy seguido.
    @GetMapping
    @Cacheable(CACHE_BRANCH_SPECIALTY)
    public List<BranchSpecialtyResponseDTO> listar() {
        return branchSpecialtyRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    // >>> INICIO FB - PASOS 7 a 9: 'Asignar' -> valida (FA03 / FA05) -> registra -> mensaje 'Especialidad asignada a la sede correctamente'.
    @PostMapping
    @CacheEvict(value = CACHE_BRANCH_SPECIALTY, allEntries = true)
    public ResponseEntity<Map<String, String>> asignar(@Valid @RequestBody BranchSpecialtyCreateDTO dto) {

        // >>> VALIDACION FA03 (nivel 1): @Valid ya rechazó sede/especialidad nulas con el mensaje del DTO (400, GlobalExceptionHandler).
        // >>> VALIDACION (nivel 2): la sede debe existir (si no -> 404). RN-CU12-01 pide solo sedes activas, pero aquí no se revisa 'estado'.
        Sucursal sucursal = sucursalRepository.findById(dto.sucursalId())
                .orElseThrow(() -> new ResourceNotFoundException("La sede indicada no existe."));

        // >>> VALIDACION: la especialidad debe existir (si no -> 404). Tampoco se revisa que esté activa.
        Especialidad especialidad = especialidadRepository.findById(dto.especialidadId())
                .orElseThrow(() -> new ResourceNotFoundException("La especialidad indicada no existe."));

        // >>> INICIO FA05 - Asignación duplicada (FB paso 8): misma sede + misma especialidad -> 'La asignación ya existe.'
        // >>> VALIDACION RN-CU12-01 (índice único): además de este if, la BD tiene UNIQUE(sucursal_id, especialidad_id).
        if (branchSpecialtyRepository.existsBySucursal_IdAndEspecialidad_Id(dto.sucursalId(), dto.especialidadId())) {
            throw new ValidationException("La asignación ya existe.");
        }

        // >>> FB PASO 9: se registra la asignación (estado = 1 activo, automático).
        BranchSpecialty entidad = new BranchSpecialty();
        entidad.setSucursal(sucursal);
        entidad.setEspecialidad(especialidad);
        entidad.setEstado((short) 1); // estado se asigna automáticamente como activo [RN-CU12-01]

        branchSpecialtyRepository.save(entidad);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("mensaje", "Especialidad asignada a la sede correctamente"));
    }

    // >>> INICIO FA02 - Eliminar asignación (DELETE físico del registro; el diálogo de confirmación es del front).
    @DeleteMapping("/{id}")
    @CacheEvict(value = CACHE_BRANCH_SPECIALTY, allEntries = true)
    public ResponseEntity<Map<String, String>> eliminar(@PathVariable Integer id) {
        // >>> VALIDACION: la asignación debe existir (si no -> 404).
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
