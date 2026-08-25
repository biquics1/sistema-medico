package com.example.demo.repository;

import com.example.demo.modelo.Laboratorio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface LaboratorioRepository extends JpaRepository<Laboratorio, Integer>,
        JpaSpecificationExecutor<Laboratorio> {

    boolean existsByNombreIgnoreCaseAndEstado(String nombre, Short estado);

    boolean existsByNombreIgnoreCaseAndEstadoAndIdNot(String nombre, Short estado, Integer id);
}
