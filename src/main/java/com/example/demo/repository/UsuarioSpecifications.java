// ============================================================
// SPECIFICATION: UsuarioSpecifications
// Filtros dinámicos reutilizables para el listado/búsqueda de
// Usuarios (CU-01, RN-CU01-01), combinables entre sí y con el
// filtro de sede del Administrador de Sede.
// ============================================================
package com.example.demo.repository;

import com.example.demo.modelo.Usuario;
import org.springframework.data.jpa.domain.Specification;

/**
 * Specifications reutilizables para acotar consultas de Usuario.
 * Pensado para el Administrador de Sede: cualquier listado/búsqueda de
 * usuarios se le combina con sucursalIdEs(sucursalId) para que solo vea
 * el personal (médicos, enfermería, etc.) de su propia sucursal.
 * El Administrador General pasa sucursalId = null -> sin filtro.
 */
public final class UsuarioSpecifications {

    private UsuarioSpecifications() {
        // Clase de utilidad: no se instancia
    }

    // Si sucursalId es null, no restringe nada (caso Administrador General).
    public static Specification<Usuario> sucursalIdEs(Integer sucursalId) {
        return (root, query, cb) -> sucursalId == null
                ? cb.conjunction()
                : cb.equal(root.get("sucursal").get("id"), sucursalId);
    }

    // ===== RN-CU01-01: filtro por campo de la pantalla "Listado de Usuarios" =====
    public static Specification<Usuario> idEs(Integer id) {
        return (root, query, cb) -> cb.equal(root.get("id"), id);
    }

    // Búsqueda parcial (LIKE) e insensible a mayúsculas por nombre completo
    public static Specification<Usuario> nombreCompletoContiene(String texto) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("nombreCompleto")), "%" + texto.toLowerCase() + "%");
    }

    // Búsqueda parcial por correo electrónico
    public static Specification<Usuario> correoContiene(String texto) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("correoElectronico")), "%" + texto.toLowerCase() + "%");
    }

    // Búsqueda parcial por nombre de usuario
    public static Specification<Usuario> nombreUsuarioContiene(String texto) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("nombreUsuario")), "%" + texto.toLowerCase() + "%");
    }

    // Búsqueda parcial por DPI
    public static Specification<Usuario> dpiContiene(String texto) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("dpi")), "%" + texto.toLowerCase() + "%");
    }

    // Búsqueda parcial por nombre de rol (join implícito Usuario -> Rol)
    public static Specification<Usuario> rolNombreContiene(String texto) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("rol").get("nombre")), "%" + texto.toLowerCase() + "%");
    }
}
