package com.example.demo.repository;

import com.example.demo.modelo.MovimientoInventario;
import org.springframework.data.jpa.domain.Specification;

/**
 * Mismo patrón que UsuarioSpecifications: acota la bitácora de movimientos
 * de inventario a la sucursal del usuario autenticado (Administrador de
 * Sede o Farmacéutico). sucursalId = null (Administrador General) -> sin
 * filtro, ve todas las sucursales.
 */
public final class MovimientoInventarioSpecifications {

    private MovimientoInventarioSpecifications() {
    }

    public static Specification<MovimientoInventario> sucursalIdEs(Integer sucursalId) {
        return (root, query, cb) -> sucursalId == null
                ? cb.conjunction()
                : cb.equal(root.get("sucursal").get("id"), sucursalId);
    }
}
