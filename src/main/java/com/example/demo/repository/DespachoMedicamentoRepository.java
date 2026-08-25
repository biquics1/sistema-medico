package com.example.demo.repository;

import com.example.demo.modelo.DespachoMedicamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DespachoMedicamentoRepository extends JpaRepository<DespachoMedicamento, Integer> {

    List<DespachoMedicamento> findByReceta_Id(Integer recetaId);
}
