package com.example.demo.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // NUEVO: se agrega el id numérico del usuario como claim del token,
    // para que JwtAuthFilter pueda armar el AuthUsuario sin ir a la BD.
    // sucursalId también viaja en el token (puede ser null) para que el
    // Administrador de Sede quede automáticamente acotado a su sucursal
    // sin tener que volver a consultar la BD en cada request.
    public String generarToken(Integer id, String nombreUsuario, String rol, Integer sucursalId) {
        Date ahora = new Date();
        Date expiracion = new Date(ahora.getTime() + expirationMs);

        var builder = Jwts.builder()
                .subject(nombreUsuario)
                .claim("id", id)
                .claim("rol", rol)
                .issuedAt(ahora)
                .expiration(expiracion);

        if (sucursalId != null) {
            builder.claim("sucursalId", sucursalId);
        }

        return builder.signWith(getKey()).compact();
    }

    public String extraerUsuario(String token) {
        return extraerClaims(token).getSubject();
    }

    public String extraerRol(String token) {
        return extraerClaims(token).get("rol", String.class);
    }

    public Integer extraerId(String token) {
        return extraerClaims(token).get("id", Integer.class);
    }

    // NUEVO: puede no venir en el token (usuarios sin sucursal, ej. Administrador General)
    public Integer extraerSucursalId(String token) {
        return extraerClaims(token).get("sucursalId", Integer.class);
    }

    public boolean esTokenValido(String token) {
        try {
            extraerClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims extraerClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
