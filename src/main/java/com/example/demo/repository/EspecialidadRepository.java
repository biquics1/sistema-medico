// ============================================================
// REPOSITORY: EspecialidadRepository
// Acceso a datos del catálogo de Especialidades (CU-00/CU-03/CU-15).
// JpaSpecificationExecutor permite construir filtros dinámicos
// (búsqueda por nombre, estado, etc.) desde el service.
// ============================================================
package com.example.demo.repository;

import com.example.demo.modelo.Especialidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

// NOTA: si tu EspecialidadRepository actual ya tiene métodos propios (además del
// findAll() usado por EspecialidadController), fusiónalos aquí en vez de sobrescribir.
public interface EspecialidadRepository extends JpaRepository<Especialidad, Integer>,
        JpaSpecificationExecutor<Especialidad> {

    // Valida nombre único al crear (RN-CU15-01)
    boolean existsByNombreIgnoreCaseAndEstado(String nombre, Short estado);

    // Valida nombre único al editar, excluyendo el propio registro
    boolean existsByNombreIgnoreCaseAndEstadoAndIdNot(String nombre, Short estado, Integer id);
}
