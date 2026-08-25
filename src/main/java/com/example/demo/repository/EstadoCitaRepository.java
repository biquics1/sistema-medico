package com.example.demo.repository;

import com.example.demo.modelo.EstadoCita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface EstadoCitaRepository extends JpaRepository<EstadoCita, Integer>,
        JpaSpecificationExecutor<EstadoCita> {

    boolean existsByNombreIgnoreCaseAndEstado(String nombre, Short estado);

    Optional<EstadoCita> findByNombre(String nombre);
}