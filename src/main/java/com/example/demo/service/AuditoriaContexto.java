package com.example.demo.service;

import com.example.demo.security.AuditContextHolder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

/**
 * Envía al motor de PostgreSQL (mismo connection/transacción del método
 * que la invoca) los valores de app.usuario_id y app.ip_origen que leen
 * los triggers creados en bitacora_general.sql.
 *
 * IMPORTANTE: debe llamarse como PRIMERA línea dentro de un método
 * anotado @Transactional (por eso NO se llama desde el filtro; el
 * filtro corre antes de que exista transacción/conexión de negocio).
 *
 * Uso típico dentro de un Service:
 *
 *   @Transactional
 *   public UsuarioResponseDTO crear(UsuarioCreateDTO dto) {
 *       auditoriaContexto.aplicar();
 *       ... resto de la lógica ...
 *   }
 */
@Component
public class AuditoriaContexto {

    @PersistenceContext
    private EntityManager em;

    public void aplicar() {
        Integer usuarioId = AuditContextHolder.getUsuarioId();
        String ip = AuditContextHolder.getIpOrigen();

        em.createNativeQuery(
                        "SELECT set_config('app.usuaio_id', :uid, true), set_config('app.ip_origen', :ip, true)")
                .setParameter("uid", usuarioId != null ? usuarioId.toString() : "")
                .setParameter("ip", ip != null ? ip : "")
                .getSingleResult();
    }
}