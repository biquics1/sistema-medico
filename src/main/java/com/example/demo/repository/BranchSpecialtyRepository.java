// ============================================================
// REPOSITORY: BranchSpecialtyRepository
// Acceso a datos de la relación Sucursal-Especialidad (CU-12/CU-13).
// Solo permite crear y eliminar (sin edición), por eso no hay
// métodos de actualización personalizados.
// ============================================================
package com.example.demo.repository;

import com.example.demo.modelo.BranchSpecialty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BranchSpecialtyRepository extends JpaRepository<BranchSpecialty, Integer> {

    // Especialidades activas de una sucursal (usado en el wizard de citas, CU-03 paso 2)
    List<BranchSpecialty> findBySucursal_IdAndEstado(Integer sucursalId, Short estado);

    // Valida duplicados antes de crear una asignación (RN-CU12-01)
    boolean existsBySucursal_IdAndEspecialidad_Id(Integer sucursalId, Integer especialidadId);
}
