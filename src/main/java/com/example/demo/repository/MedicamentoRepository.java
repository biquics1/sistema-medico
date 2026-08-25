package com.example.demo.repository;

import com.example.demo.modelo.Medicamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicamentoRepository extends JpaRepository<Medicamento, Integer> {

    List<Medicamento> findByEstado(Short estado);

    // CU-11 (extensión): catálogo/búsqueda de medicamentos en Farmacia (venta libre)
    List<Medicamento> findByEstadoAndNombreContainingIgnoreCase(Short estado, String nombre);

    // NUEVO: validar nombre único al crear un medicamento (RN-CU15-01)
    boolean existsByNombreIgnoreCaseAndEstado(String nombre, Short estado);
}
