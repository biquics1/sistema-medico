// ============================================================
// REPOSITORY: DetalleDespachoMedicamentoRepository
// Acceso a datos de las líneas (medicamentos) de un despacho (CU-11).
// ============================================================
package com.example.demo.repository;

import com.example.demo.modelo.DetalleDespachoMedicamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DetalleDespachoMedicamentoRepository extends JpaRepository<DetalleDespachoMedicamento, Integer> {

    // Todos los medicamentos entregados dentro de un despacho específico
    List<DetalleDespachoMedicamento> findByDespacho_Id(Integer despachoId);
}
