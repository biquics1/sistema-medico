// ============================================================
// REPOSITORY: EstadoCitaRepository
// Acceso a datos del catálogo de Estados de Cita (CU-03/CU-05).
// ============================================================
package com.example.demo.repository;

import com.example.demo.modelo.EstadoCita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface EstadoCitaRepository extends JpaRepository<EstadoCita, Integer>,
        JpaSpecificationExecutor<EstadoCita> {

    // Valida nombre único al crear/editar (RN-CU15-01)
    boolean existsByNombreIgnoreCaseAndEstado(String nombre, Short estado);

    // Busca un estado por su nombre exacto (ej. "Confirmada") para asignarlo a una Cita
    Optional<EstadoCita> findByNombre(String nombre);
}
