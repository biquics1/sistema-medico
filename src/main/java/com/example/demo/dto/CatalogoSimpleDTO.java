package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

// DTO genérico (id + nombre) para poblar dropdowns de catálogos simples
// (ej. sucursales, especialidades) sin exponer toda la entidad.
@Data
@AllArgsConstructor
public class CatalogoSimpleDTO {
    private Integer id;
    private String nombre;
}