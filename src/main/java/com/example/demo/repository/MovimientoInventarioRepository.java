// ============================================================
// REPOSITORY: MovimientoInventarioRepository
// Acceso a datos de la bitácora de movimientos de inventario (CU-13).
// ============================================================
package com.example.demo.repository;

import com.example.demo.modelo.MovimientoInventario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Integer>,
        JpaSpecificationExecutor<MovimientoInventario> {

    // Movimientos de un medicamento/sucursal dentro de un rango de fechas (resumen mensual, RN-CU13-03)
    @Query("SELECT m FROM MovimientoInventario m " +
            "WHERE m.medicamento.id = :medicamentoId AND m.sucursal.id = :sucursalId " +
            "AND m.creadoEn BETWEEN :inicio AND :fin AND m.activo = true")
    List<MovimientoInventario> findResumenMensual(@Param("medicamentoId") Integer medicamentoId,
                                                  @Param("sucursalId") Integer sucursalId,
                                                  @Param("inicio") LocalDateTime inicio,
                                                  @Param("fin") LocalDateTime fin);
}
