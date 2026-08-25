package com.example.demo.repository;

import com.example.demo.modelo.CitaSeguimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CitaSeguimientoRepository extends JpaRepository<CitaSeguimiento, Integer> {

    Optional<CitaSeguimiento> findByCitaNueva_Id(Integer citaId);

    boolean existsByConsultaOrigen_Id(Integer consultaId);

    // RN-CU11-05: recordatorio 1-2 días antes, una sola vez, sobrevive a reinicios (RNF-020)
    List<CitaSeguimiento> findByRecordatorioEnviadoFalseAndCitaNueva_FechaHoraBetween(
            LocalDateTime desde, LocalDateTime hasta);
}
