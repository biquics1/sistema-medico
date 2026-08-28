package com.example.demo.security;

/**
 * Guarda por hilo (por petición HTTP) quién está haciendo la operación
 * y desde qué IP, para que la bitácora general (triggers de BD) pueda
 * registrar "quién" y "desde dónde" en cada INSERT/UPDATE/DELETE.
 *
 * Se llena en JwtAuthFilter al validar el token y se limpia siempre
 * al final del request (finally) para evitar fugas entre peticiones
 * cuando el hilo se reutiliza (thread pool del servidor).
 */
public class AuditContextHolder {

    private static final ThreadLocal<Integer> USUARIO_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> IP_ORIGEN = new ThreadLocal<>();

    private AuditContextHolder() {
    }

    public static void establecer(Integer usuarioId, String ip) {
        USUARIO_ID.set(usuarioId);
        IP_ORIGEN.set(ip);
    }

    public static Integer getUsuarioId() {
        return USUARIO_ID.get();
    }

    public static String getIpOrigen() {
        return IP_ORIGEN.get();
    }

    public static void limpiar() {
        USUARIO_ID.remove();
        IP_ORIGEN.remove();
    }
}