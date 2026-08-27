// ============================================================
// REPOSITORY: ExamenLaboratorioRepository
// Acceso a datos del catálogo de Exámenes de Laboratorio (CU-09/CU-15).
// ============================================================
package com.example.demo.repository;

import com.example.demo.modelo.ExamenLaboratorio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ExamenLaboratorioRepository extends JpaRepository<ExamenLaboratorio, Integer>,
        JpaSpecificationExecutor<ExamenLaboratorio> {

    // Valida nombre único al crear/editar (RN-CU15-01)
    boolean existsByNombreIgnoreCaseAndEstado(String nombre, Short estado);

    // Lista de exámenes activos, usada al generar una orden de laboratorio (CU-08, FA01)
    List<ExamenLaboratorio> findByEstado(Short estado);
}
