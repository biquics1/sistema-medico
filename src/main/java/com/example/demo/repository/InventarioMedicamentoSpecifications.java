// ============================================================
// SPECIFICATION: InventarioMedicamentoSpecifications
// Filtro dinámico reutilizable para acotar el inventario a la
// sucursal del usuario autenticado (Administrador de Sede).
// Si sucursalId es null (Administrador General/Farmacéutico
// sin restricción), no se aplica ningún filtro.
// ============================================================
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
        // Clase de utilidad: no se instancia
    }

    public static Specification<InventarioMedicamento> sucursalIdEs(Integer sucursalId) {
        // Si sucursalId es null -> cb.conjunction() = condición siempre verdadera (sin filtro)
        // Si no es null -> filtra por sucursal.id = sucursalId
        return (root, query, cb) -> sucursalId == null
                ? cb.conjunction()
                : cb.equal(root.get("sucursal").get("id"), sucursalId);
    }
}
