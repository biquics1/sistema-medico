// ============================================================
// REPOSITORY: InventarioMedicamentoRepository
// Acceso a datos del stock de medicamentos por sucursal.
// Incluye bloqueos (locks) para proteger operaciones
// concurrentes de descuento de stock (CU-11/CU-13, RNF-025).
// ============================================================
package com.example.demo.repository;

import com.example.demo.modelo.InventarioMedicamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface InventarioMedicamentoRepository extends JpaRepository<InventarioMedicamento, Integer>,
        JpaSpecificationExecutor<InventarioMedicamento> {

    // Busca el registro de stock de un medicamento en una sucursal específica
    Optional<InventarioMedicamento> findByMedicamentoIdAndSucursalId(Integer medicamentoId, Integer sucursalId);

    // Lock OPTIMISTA: se apoya en el campo @Version de la entidad (control de concurrencia)
    @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT i FROM InventarioMedicamento i WHERE i.id = :id")
    Optional<InventarioMedicamento> findByIdForUpdate(@Param("id") Integer id);

    // Usado por FarmaciaService antes de descontar stock en despachos concurrentes.
    // Requiere lock PESIMISTA (no OPTIMISTIC): al despachar dos usuarios el mismo
    // medicamento/sucursal al mismo tiempo, uno debe esperar a que el otro libere
    // la fila en vez de fallar por versión desactualizada.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM InventarioMedicamento i WHERE i.medicamento.id = :medicamentoId AND i.sucursal.id = :sucursalId")
    Optional<InventarioMedicamento> findForUpdate(@Param("medicamentoId") Integer medicamentoId,
                                                  @Param("sucursalId") Integer sucursalId);

    // Todos los medicamentos cuyo stock actual ya llegó al mínimo configurado (alerta de reorden)
    @Query("SELECT i FROM InventarioMedicamento i " +
            "WHERE i.medicamento.stockMinimo IS NOT NULL " +
            "AND i.stockActual <= i.medicamento.stockMinimo")
    List<InventarioMedicamento> findAllStockBajo();
}
