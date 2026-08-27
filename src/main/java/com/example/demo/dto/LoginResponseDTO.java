package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Respuesta del login: incluye el JWT y los datos básicos de sesión (rol y sede)
// que el frontend usa para pintar el header y aplicar el scope de sucursal.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class
LoginResponseDTO {
    private Integer id;
    private String token;
    private String nombreUsuario;
    private String nombreCompleto;
    private String rol;
    private Integer sucursalId;      // NUEVO: null si el usuario no está atado a una sede
    private String sucursalNombre;   // NUEVO: para mostrarlo en el header del panel
}