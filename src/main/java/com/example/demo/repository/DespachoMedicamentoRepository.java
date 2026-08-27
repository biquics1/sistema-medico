// ============================================================
// REPOSITORY: DespachoMedicamentoRepository
// Acceso a datos de la cabecera de despacho de farmacia (CU-11).
// ============================================================
package com.example.demo.repository;

import com.example.demo.modelo.DespachoMedicamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DespachoMedicamentoRepository extends JpaRepository<DespachoMedicamento, Integer> {

    // Todos los despachos generados a partir de una receta específica
    List<DespachoMedicamento> findByReceta_Id(Integer recetaId);
}
