package com.example.demo.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

// Filtro que se ejecuta una vez por request (antes de llegar al controller):
// lee el header "Authorization: Bearer <token>", valida el JWT y, si es
// válido, arma el AuthUsuario y lo registra en el SecurityContext para que
// @AuthenticationPrincipal y @PreAuthorize funcionen en el resto de la cadena.
//
// NUEVO (bitácora general): además guarda el id del usuario autenticado y
// la IP real del request en AuditContextHolder (ThreadLocal), para que los
// services puedan mandarlos a PostgreSQL vía AuditoriaContexto.aplicar()
// y los triggers de bitacora_general.sql registren "quién" y "desde dónde".
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        Integer usuarioIdParaAuditoria = null;

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            System.out.println("=== JWT recibido: " + token);

            try {
                boolean valido = jwtUtil.esTokenValido(token);
                System.out.println("=== ¿Válido?: " + valido);

                if (valido) {
                    Integer id = jwtUtil.extraerId(token);
                    String usuario = jwtUtil.extraerUsuario(token);
                    String rol = jwtUtil.extraerRol(token);
                    Integer sucursalId = jwtUtil.extraerSucursalId(token);
                    System.out.println("=== id=" + id + " usuario=" + usuario + " rol=[" + rol + "] sucursalId=" + sucursalId);

                    AuthUsuario principal = new AuthUsuario(id, usuario, rol, sucursalId);
                    var authToken = new UsernamePasswordAuthenticationToken(
                            principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + rol.toUpperCase())));
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    usuarioIdParaAuditoria = id;
                }
            } catch (Exception e) {
                System.out.println("=== ERROR validando token: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            }
        } else {
            System.out.println("=== Sin header Authorization o no empieza con 'Bearer '. Header recibido: " + header);
        }

        // Registra usuario + IP de este request para la bitácora general.
        // Si no hay token válido, usuarioIdParaAuditoria queda en null: el
        // trigger igual guarda la operación, solo sin usuario asociado.
        AuditContextHolder.establecer(usuarioIdParaAuditoria, obtenerIpReal(request));

        try {
            filterChain.doFilter(request, response);
        } finally {
            // Limpieza obligatoria: el hilo se reutiliza para otros requests
            // (thread pool de Tomcat), así que nunca debe quedar "pegado" el
            // usuario/IP de una petición anterior.
            AuditContextHolder.limpiar();
        }
    }

    // Soporta el caso de estar detrás de un proxy/nginx/balanceador: en ese
    // escenario request.getRemoteAddr() devolvería la IP del proxy, no la
    // del cliente real, por eso se prioriza X-Forwarded-For si existe.
    private String obtenerIpReal(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            return request.getRemoteAddr();
        }
        return ip.split(",")[0].trim();
    }
}