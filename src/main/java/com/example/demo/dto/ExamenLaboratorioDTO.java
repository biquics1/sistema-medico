package com.example.demo.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

// Contenedor de DTOs del catálogo "Exámenes de Laboratorio" (CU-15).
public class ExamenLaboratorioDTO {

    // Body para crear/actualizar un examen de laboratorio (precio, rango de
    // referencia, unidad y laboratorio al que pertenece).
    @Data
    public static class CreateDTO {
        @Size(max = 20, message = "El código no puede exceder los 20 caracteres.")
        private String codigo;

        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 200, message = "El nombre no puede exceder los 200 caracteres.")
        private String nombre;

        @NotNull(message = "El precio base debe ser mayor a 0.")
        @DecimalMin(value = "0.01", message = "El precio base debe ser mayor a 0.")
        private BigDecimal precio;

        @Size(max = 100)
        private String rangoReferencia;

        @Size(max = 30)
        private String unidadMedida;

        @NotNull(message = "Debe seleccionar un laboratorio.")
        private Integer laboratorioId;

        @NotNull(message = "Debe seleccionar un estado.")
        private Short estado;
    }

    // Datos del examen para el listado, con el nombre del laboratorio ya resuelto.
    @Data
    public static class ResponseDTO {
        private Integer id;
        private String codigo;
        private String nombre;
        private BigDecimal precio;
        private String rangoReferencia;
        private String unidadMedida;
        private Integer laboratorioId;
        private String laboratorioNombre;
        private Short estado;
    }
}
