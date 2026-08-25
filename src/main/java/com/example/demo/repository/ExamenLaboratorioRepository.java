package com.example.demo.repository;

import com.example.demo.modelo.ExamenLaboratorio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ExamenLaboratorioRepository extends JpaRepository<ExamenLaboratorio, Integer>,
        JpaSpecificationExecutor<ExamenLaboratorio> {

    boolean existsByNombreIgnoreCaseAndEstado(String nombre, Short estado);

    List<ExamenLaboratorio> findByEstado(Short estado);
}