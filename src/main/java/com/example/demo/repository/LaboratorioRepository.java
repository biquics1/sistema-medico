// ============================================================
// REPOSITORY: LaboratorioRepository
// Acceso a datos del catálogo de Laboratorios (CU-09/CU-15).
// ============================================================
package com.example.demo.repository;

import com.example.demo.modelo.Laboratorio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface LaboratorioRepository extends JpaRepository<Laboratorio, Integer>,
        JpaSpecificationExecutor<Laboratorio> {

    // Valida nombre único al crear (RN-CU15-01)
    boolean existsByNombreIgnoreCaseAndEstado(String nombre, Short estado);

    // Valida nombre único al editar, excluyendo el propio registro
    boolean existsByNombreIgnoreCaseAndEstadoAndIdNot(String nombre, Short estado, Integer id);
}
