// ============================================================
// REPOSITORY: RecetaMedicaRepository
// Acceso a datos de las recetas médicas (CU-08/CU-11).
// ============================================================
package com.example.demo.repository;

import com.example.demo.modelo.RecetaMedica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecetaMedicaRepository extends JpaRepository<RecetaMedica, Integer> {

    // CU-11 paso 2 FB: búsqueda por ID de receta, solo activas (state = 1)
    Optional<RecetaMedica> findByIdAndEstado(Integer id, Short estado);

    // CU-11 paso 2 FB: búsqueda por ID de consulta, solo activas (state = 1)
    List<RecetaMedica> findByConsulta_IdAndEstado(Integer consultaId, Short estado);
}
