// ============================================================
// REPOSITORY: SucursalRepository
// Acceso a datos del catálogo de Sucursales (CU-00/CU-03/CU-12/CU-15).
// ============================================================
package com.example.demo.repository;

import com.example.demo.modelo.Sucursal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

// NOTA: si tu SucursalRepository actual ya tiene métodos propios (además del
// findAll() usado por SucursalController), fusiónalos aquí en vez de sobrescribir.
public interface SucursalRepository extends JpaRepository<Sucursal, Integer>,
        JpaSpecificationExecutor<Sucursal> {

    // Usado por CitaService.listarSucursalesActivas() (CU-03, paso 1 del wizard)
    List<Sucursal> findByEstado(Short estado);

    // Valida nombre único al crear (RN-CU15-01)
    boolean existsByNombreIgnoreCaseAndEstado(String nombre, Short estado);

    // Valida nombre único al editar, excluyendo el propio registro
    boolean existsByNombreIgnoreCaseAndEstadoAndIdNot(String nombre, Short estado, Integer id);
}
