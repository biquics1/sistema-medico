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
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");


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
                }
            } catch (Exception e) {
                System.out.println("=== ERROR validando token: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            }
        } else {
            System.out.println("=== Sin header Authorization o no empieza con 'Bearer '. Header recibido: " + header);
        }

        filterChain.doFilter(request, response);
    }
}
