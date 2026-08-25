package com.example.demo.security;

import java.security.Principal;

/**
 * Principal que queda disponible vía @AuthenticationPrincipal en los
 * controllers una vez que el JWT es válido. Reemplaza el patrón anterior
 * de recibir idCajero / medicoId / idEnfermero / idFarmaceutico como
 * @RequestParam o dentro del body: ahora ese id sale directamente del
 * token, no de lo que el cliente decida enviar.
 */
public class AuthUsuario implements Principal {

    // Nombre EXACTO del rol que representa al admin acotado a una sola sede.
    // Debe coincidir con rol.nombre en la base de datos.
    private static final String ROL_ADMIN_SEDE = "Administrador";
    private static final String ROL_ADMIN_GENERAL = "Administrador General";

    private final Integer id;
    private final String nombreUsuario;
    private final String rol;
    private final Integer sucursalId; // NUEVO: null si el usuario no está atado a una sede

    public AuthUsuario(Integer id, String nombreUsuario, String rol, Integer sucursalId) {
        this.id = id;
        this.nombreUsuario = nombreUsuario;
        this.rol = rol;
        this.sucursalId = sucursalId;
    }

    public Integer getId() {
        return id;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public String getRol() {
        return rol;
    }

    public Integer getSucursalId() {
        return sucursalId;
    }

    public boolean esAdminSede() {
        return ROL_ADMIN_SEDE.equalsIgnoreCase(rol);
    }

    public boolean esAdminGeneral() {
        return ROL_ADMIN_GENERAL.equalsIgnoreCase(rol);
    }

    /**
     * Devuelve el id de sucursal por el que hay que filtrar la consulta,
     * o null si no hay que filtrar.
     *
     * CORREGIDO: antes solo el rol "Administrador" (Admin de Sede) quedaba
     * acotado; cualquier otro rol atado a una sola sucursal (Farmacéutico,
     * Cajero, Enfermero, Laboratorista, Recepcionista) devolvía null aquí y
     * por lo tanto veía el inventario/datos de TODAS las sucursales, aunque
     * en la tabla `usuario` sí tuviera una sola sucursal_id asignada. Ahora
     * el criterio es: "Administrador General" ve todo (null); cualquier
     * otro rol con sucursalId asignado queda acotado a esa sucursal; un rol
     * sin sucursalId asignado (ej. Paciente) simplemente no filtra nada
     * porque no llama a estos endpoints acotados por sede.
     *
     * Úsalo en los services que necesiten "ver solo mi sede".
     */
    public Integer sucursalScopeOrNull() {
        return esAdminGeneral() ? null : sucursalId;
    }

    // Para que Authentication.getName() siga devolviendo el nombre de
    // usuario, igual que cuando el principal era un String plano.
    @Override
    public String getName() {
        return nombreUsuario;
    }
}
