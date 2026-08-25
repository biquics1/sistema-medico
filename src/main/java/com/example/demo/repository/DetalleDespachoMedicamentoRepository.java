package com.example.demo.repository;

import com.example.demo.modelo.DetalleDespachoMedicamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DetalleDespachoMedicamentoRepository extends JpaRepository<DetalleDespachoMedicamento, Integer> {

    List<DetalleDespachoMedicamento> findByDespacho_Id(Integer despachoId);
}
