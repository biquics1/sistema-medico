// ============================================================
// REPOSITORY: OrdenLaboratorioRepository
// Acceso a datos de las órdenes de laboratorio (CU-08/CU-09/CU-16).
// ============================================================
package com.example.demo.repository;

import com.example.demo.modelo.OrdenLaboratorio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrdenLaboratorioRepository extends JpaRepository<OrdenLaboratorio, Integer> {

    // CU-09 paso 1 FB: listado general, más recientes primero
    List<OrdenLaboratorio> findAllByOrderByCreadoEnDesc();

    // CU-16/CU-10 doc: búsqueda de orden pendiente de pago por DPI del paciente
    List<OrdenLaboratorio> findByPaciente_DpiAndEstado(String dpi, Short estado);

    // CU-16/CU-10 doc: búsqueda de orden pendiente de pago por número de orden
    Optional<OrdenLaboratorio> findByIdAndEstado(Integer id, Short estado);

    // NUEVO — Dashboard del Paciente: la orden de laboratorio generada en una consulta específica.
    Optional<OrdenLaboratorio> findByConsulta_Id(Integer consultaId);
}
