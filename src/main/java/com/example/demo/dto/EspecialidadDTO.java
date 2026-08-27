package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

// Contenedor de DTOs del catálogo "Especialidades" (CU-15): datos de creación/edición
// y datos de respuesta para el listado.
public class EspecialidadDTO {

    // Body para crear/actualizar una especialidad.
    @Data
    public static class CreateDTO {
        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 200, message = "El nombre no puede exceder los 200 caracteres.")
        private String nombre;

        // RN-CU15-01: Descripción obligatoria en Especialidades y Medicamentos.
        @NotBlank(message = "La descripción es obligatoria.")
        @Size(max = 500, message = "La descripción no puede exceder los 500 caracteres.")
        private String descripcion;

        @NotNull(message = "Debe seleccionar un estado.")
        private Short estado;
    }

    // Datos de una especialidad para mostrar en listados/dropdowns.
    @Data
    public static class ResponseDTO {
        private Integer id;
        private String nombre;
        private String descripcion;
        private Short estado;
    }
}
