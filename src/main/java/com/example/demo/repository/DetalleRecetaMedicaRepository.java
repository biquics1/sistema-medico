package com.example.demo.repository;

import com.example.demo.modelo.DetalleRecetaMedica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DetalleRecetaMedicaRepository extends JpaRepository<DetalleRecetaMedica, Integer> {

    // CU-11 paso 4 FB: medicamentos recetados a mostrar antes de despachar
    List<DetalleRecetaMedica> findByReceta_Id(Integer recetaId);
}
