package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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