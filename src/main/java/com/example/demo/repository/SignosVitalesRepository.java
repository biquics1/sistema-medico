// ============================================================
// REPOSITORY: SignosVitalesRepository
// Acceso a datos de los signos vitales tomados por enfermería (CU-07).
// ============================================================
package com.example.demo.repository;

import com.example.demo.modelo.SignosVitales;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SignosVitalesRepository extends JpaRepository<SignosVitales, Integer> {

    // Obtiene el registro de signos vitales de una cita (relación 1:1)
    Optional<SignosVitales> findByCita_Id(Integer citaId);

    // Verifica si una cita ya tiene signos vitales registrados
    boolean existsByCita_Id(Integer citaId);
}
