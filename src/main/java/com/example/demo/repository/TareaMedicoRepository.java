// ============================================================
// REPOSITORY: TareaMedicoRepository
// Acceso a datos de las tareas personales del médico (CU-14, TaskPanel).
// ============================================================
package com.example.demo.repository;

import com.example.demo.modelo.TareaMedico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TareaMedicoRepository extends JpaRepository<TareaMedico, Integer> {

    // Todas las tareas de un médico, ordenadas por fecha límite
    List<TareaMedico> findByMedico_IdOrderByFechaLimiteAsc(Integer medicoId);

    // Tareas de un médico filtradas por estado (Pendientes / Completadas)
    List<TareaMedico> findByMedico_IdAndCompletadaOrderByFechaLimiteAsc(Integer medicoId, Boolean completada);

    // Para validar que el médico solo edite/elimine/complete sus propias tareas.
    Optional<TareaMedico> findByIdAndMedico_Id(Integer id, Integer medicoId);
}
