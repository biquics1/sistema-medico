package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

// DTO usado en el panel administrativo de gestión de médicos por sede.
// Médico visible en el panel de "Control de Citas por Sede" del Administrador.
// Administrador de Sede: solo ve médicos de su propia sucursal.
// Administrador General: ve médicos de todas las sedes.
@Data
@AllArgsConstructor
public class AdminMedicoDTO {
    private Integer id;
    private String nombreCompleto;
    private String correoElectronico;
    private String especialidadNombre;
    private Integer sucursalId;
    private String sucursalNombre;
    private Short estado;
}
