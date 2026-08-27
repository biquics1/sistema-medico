// ============================================================
// REPOSITORY: ConsultaMedicaRepository
// Acceso a datos de las consultas médicas (CU-08).
// ============================================================
package com.example.demo.repository;

import com.example.demo.modelo.ConsultaMedica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConsultaMedicaRepository extends JpaRepository<ConsultaMedica, Integer> {

    // Obtiene la consulta médica asociada a una cita (relación 1:1)
    Optional<ConsultaMedica> findByCita_Id(Integer citaId);
}
