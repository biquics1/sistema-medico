package com.example.demo.repository;

import com.example.demo.modelo.EventoAgenda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventoAgendaRepository extends JpaRepository<EventoAgenda, Integer> {

    // Eventos que se solapan con el rango [desde, hasta] visible en el calendario.
    List<EventoAgenda> findByMedico_IdAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqualOrderByFechaInicioAsc(
            Integer medicoId, LocalDateTime hasta, LocalDateTime desde);

    // Para validar que el médico solo edite/elimine sus propios eventos.
    Optional<EventoAgenda> findByIdAndMedico_Id(Integer id, Integer medicoId);
}