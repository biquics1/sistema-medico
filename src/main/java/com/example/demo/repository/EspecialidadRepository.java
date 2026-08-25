package com.example.demo.repository;

import com.example.demo.modelo.Especialidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

// NOTA: si tu EspecialidadRepository actual ya tiene métodos propios (además del
// findAll() usado por EspecialidadController), fusiónalos aquí en vez de sobrescribir.
public interface EspecialidadRepository extends JpaRepository<Especialidad, Integer>,
        JpaSpecificationExecutor<Especialidad> {

    boolean existsByNombreIgnoreCaseAndEstado(String nombre, Short estado);

    boolean existsByNombreIgnoreCaseAndEstadoAndIdNot(String nombre, Short estado, Integer id);
}
