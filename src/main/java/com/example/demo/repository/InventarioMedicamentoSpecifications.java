package com.example.demo.repository;

import com.example.demo.modelo.InventarioMedicamento;
import org.springframework.data.jpa.domain.Specification;

/**
 * Mismo patrón que UsuarioSpecifications: acota el listado de inventario
 * a la sucursal del Administrador de Sede. sucursalId = null (Administrador
 * General / Farmacéutico sin restricción) -> sin filtro.
 */
public final class InventarioMedicamentoSpecifications {

    private InventarioMedicamentoSpecifications() {
    }

    public static Specification<InventarioMedicamento> sucursalIdEs(Integer sucursalId) {
        return (root, query, cb) -> sucursalId == null
                ? cb.conjunction()
                : cb.equal(root.get("sucursal").get("id"), sucursalId);
    }
}
