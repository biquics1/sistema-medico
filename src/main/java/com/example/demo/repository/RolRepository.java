// ============================================================
// REPOSITORY: RolRepository
// Acceso a datos del catálogo de Roles (CU-01).
// ============================================================
package com.example.demo.repository;

import com.example.demo.modelo.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

// NOTA: si tu RolRepository actual ya tiene métodos propios (además del
// findAll() usado por RolController), fusiónalos aquí en vez de sobrescribir.
public interface RolRepository extends JpaRepository<Rol, Integer>,
        JpaSpecificationExecutor<Rol> {

    // Valida nombre único al crear (RN-CU15-01)
    boolean existsByNombreIgnoreCaseAndEstado(String nombre, Short estado);

    // Valida nombre único al editar, excluyendo el propio registro
    boolean existsByNombreIgnoreCaseAndEstadoAndIdNot(String nombre, Short estado, Integer id);
}
