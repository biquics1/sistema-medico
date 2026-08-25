package com.example.demo.repository;

import com.example.demo.modelo.DetalleOrdenLaboratorio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DetalleOrdenLaboratorioRepository extends JpaRepository<DetalleOrdenLaboratorio, Integer> {

    List<DetalleOrdenLaboratorio> findByOrden_Id(Integer ordenId);

    Optional<DetalleOrdenLaboratorio> findByIdAndOrden_Id(Integer id, Integer ordenId);
}
