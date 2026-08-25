package com.example.demo.repository;

import com.example.demo.modelo.TareaMedico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TareaMedicoRepository extends JpaRepository<TareaMedico, Integer> {

    List<TareaMedico> findByMedico_IdOrderByFechaLimiteAsc(Integer medicoId);

    List<TareaMedico> findByMedico_IdAndCompletadaOrderByFechaLimiteAsc(Integer medicoId, Boolean completada);

    // Para validar que el médico solo edite/elimine/complete sus propias tareas.
    Optional<TareaMedico> findByIdAndMedico_Id(Integer id, Integer medicoId);
}