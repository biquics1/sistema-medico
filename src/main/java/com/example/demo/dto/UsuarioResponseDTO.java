package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Datos de un usuario interno para el listado de "Usuarios" (CU-01), con rol,
// sucursal y especialidad ya resueltos a nombre legible.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioResponseDTO {
    private Integer id;
    private String nombreCompleto;
    private String correoElectronico;
    private String nombreUsuario;
    private String dpi;
    private String telefono;
    private String rolNombre;
    private Integer sucursalId;
    private String sucursalNombre;
    private String especialidadNombre;
    private Short estado;
}