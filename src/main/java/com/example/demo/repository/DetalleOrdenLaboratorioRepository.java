// ============================================================
// REPOSITORY: DetalleOrdenLaboratorioRepository
// Acceso a datos de los exámenes dentro de una orden de
// laboratorio, incluyendo sus resultados (CU-09).
// ============================================================
package com.example.demo.repository;

import com.example.demo.modelo.DetalleOrdenLaboratorio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DetalleOrdenLaboratorioRepository extends JpaRepository<DetalleOrdenLaboratorio, Integer> {

    // Todos los exámenes de una orden (para mostrar el detalle completo)
    List<DetalleOrdenLaboratorio> findByOrden_Id(Integer ordenId);

    // Valida que el examen (detalle) pertenezca a la orden indicada antes de operar sobre él
    Optional<DetalleOrdenLaboratorio> findByIdAndOrden_Id(Integer id, Integer ordenId);
}
