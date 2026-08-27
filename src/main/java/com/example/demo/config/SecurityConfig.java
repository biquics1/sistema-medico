package com.example.demo.config;

import com.example.demo.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// Configuración central de Spring Security: define qué rutas son públicas, qué
// rol(es) puede acceder a cada módulo (autorización a nivel de ruta), registra
// el filtro JWT y expone el encoder de contraseñas (BCrypt). La autorización más
// fina (@PreAuthorize) y el filtrado por sede se resuelven en cada controller/service.
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    // Nombres de rol tal como están en la tabla `rol` (JwtAuthFilter arma
    // la autoridad como "ROLE_" + rol.toUpperCase()).
    private static final String ROLE_ADMIN = "ROLE_ADMINISTRADOR";
    private static final String ROLE_ADMIN_GENERAL = "ROLE_ADMINISTRADOR GENERAL";
    private static final String ROLE_MEDICO = "ROLE_MÉDICO";
    private static final String ROLE_ENFERMERO = "ROLE_ENFERMERO";
    private static final String ROLE_RECEPCIONISTA = "ROLE_RECEPCIONISTA";
    private static final String ROLE_CAJERO = "ROLE_CAJERO";
    private static final String ROLE_LABORATORISTA = "ROLE_LABORATORISTA";
    private static final String ROLE_FARMACEUTICO = "ROLE_FARMACÉUTICO";

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Front-end estático de prueba
                        .requestMatchers("/", "/*.html", "/*.css", "/*.js", "/favicon.ico").permitAll()

                        .requestMatchers("/error").permitAll()

                        // CU-00 / CU-02: acceso público antes de iniciar sesión
                        .requestMatchers("/api/register", "/api/verify-dpi", "/api/auth/login").permitAll()

                        // CU-00: catálogo público (portal muestra sedes/especialidades sin login)
                        .requestMatchers(HttpMethod.GET,
                                "/api/branches", "/api/branches/*/specialties",
                                "/api/especialidades/**", "/api/sucursales/**").permitAll()

                        // CU-01: mantenimiento de usuarios -> Administrador (de sede, acotado
                        // a su sucursal dentro del service) y Administrador General (todo)
                        .requestMatchers("/api/users/**").hasAnyAuthority(ROLE_ADMIN, ROLE_ADMIN_GENERAL)

                        // NUEVO: panel del Administrador de Sede / Administrador General:
                        // ver médicos de la(s) sede(s) y su agenda de citas (control de citas)
                        .requestMatchers("/api/admin/**").hasAnyAuthority(ROLE_ADMIN, ROLE_ADMIN_GENERAL)

                        // CU-03 / CU-04: elegir médico/horario y agendar/pagar -> paciente autenticado
                        .requestMatchers(
                                "/api/branches/*/specialties/*/doctors",
                                "/api/doctors/*/available-slots",
                                "/api/appointments/**",
                                "/api/payments").authenticated()

                        // CU-05: recepción -> operativo de sede. El Administrador de Sede vuelve
                        // a tener acceso, pero acotado a su propia sucursal (filtrado real en
                        // RecepcionService usando AuthUsuario.sucursalScopeOrNull()).
                        .requestMatchers("/api/recepcion/**").hasAnyAuthority(ROLE_RECEPCIONISTA, ROLE_ADMIN, ROLE_ADMIN_GENERAL)

                        // CU-06 / CU-10: caja -> igual: Administrador de Sede ve/cobra solo lo de su sede.
                        .requestMatchers("/api/caja/**").hasAnyAuthority(ROLE_CAJERO, ROLE_ADMIN, ROLE_ADMIN_GENERAL)

                        // CU-07: enfermería -> igual, acotado a la sede del Administrador.
                        .requestMatchers("/api/enfermeria/**").hasAnyAuthority(ROLE_ENFERMERO, ROLE_ADMIN, ROLE_ADMIN_GENERAL)

                        // CU-08: consulta médica -> panel de trabajo propio del médico (no aplica
                        // acotar por sede porque no es una vista de "solo consulta" del Administrador).
                        .requestMatchers("/api/medico/**").hasAnyAuthority(ROLE_MEDICO, ROLE_ADMIN_GENERAL)

                        // CU-09: laboratorio -> Administrador de Sede ve solo las órdenes cuyo
                        // médico solicitante pertenece a su sede (no hay sucursal_id directo en
                        // la orden, se resuelve vía orden.medico.sucursal en LaboratorioService).
                        .requestMatchers("/api/laboratorio/**").hasAnyAuthority(ROLE_LABORATORISTA, ROLE_ADMIN, ROLE_ADMIN_GENERAL)

                        // CU-11: farmacia -> Administrador de Sede vuelve a tener acceso, acotado
                        // a la sucursal desde la que despacha/consulta (validado en FarmaciaService).
                        .requestMatchers("/api/farmacia/**").hasAnyAuthority(ROLE_FARMACEUTICO, ROLE_ADMIN, ROLE_ADMIN_GENERAL)

                        // Catálogos de solo lectura restantes (roles, medicamentos)
                        .requestMatchers(HttpMethod.GET, "/api/roles/**", "/api/medicamentos/**").permitAll()
                        // Sedes/especialidades: configuración de catálogo global -> solo Admin General
                        .requestMatchers("/api/branch-specialty/**").hasAuthority(ROLE_ADMIN_GENERAL)
                        // Bitácora/inventario de medicamentos: Administrador de Sede y Farmacéutico
                        // vuelven a tener acceso, acotado a su propia sucursal (InventarioMedicamentoService /
                        // MovimientoInventarioService). El @PreAuthorize de cada endpoint valida el rol exacto.
                        .requestMatchers("/api/inventario-medicamentos/**", "/api/movimientos-inventario/**")
                        .hasAnyAuthority(ROLE_ADMIN, ROLE_ADMIN_GENERAL, ROLE_FARMACEUTICO)

                        // Todo lo demás requiere sesión válida
                        .anyRequest().authenticated()
                )
                // TEMPORAL - solo para diagnosticar el 403 de CU-12. Quitar cuando ya no se necesite.
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write(
                                    "{\"mensaje\":\"NO AUTENTICADO. " +
                                            "El token no llegó, no es válido o expiró. Detalle: " +
                                            authException.getMessage().replace("\"", "'") + "\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            var auth = SecurityContextHolder.getContext().getAuthentication();
                            String authorities = (auth != null) ? auth.getAuthorities().toString() : "ninguna (no autenticado)";
                            String principal = (auth != null) ? String.valueOf(auth.getPrincipal()) : "ninguno";
                            response.setStatus(403);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write(
                                    "{\"mensaje\":\"ACCESO DENEGADO. Ruta: " + request.getRequestURI() +
                                            " | Metodo: " + request.getMethod() +
                                            " | Authorities actuales: " + authorities.replace("\"", "'") +
                                            " | Principal: " + principal.replace("\"", "'") + "\"}");
                        })
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}