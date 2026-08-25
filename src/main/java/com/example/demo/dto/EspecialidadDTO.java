package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

public class EspecialidadDTO {

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

    @Data
    public static class ResponseDTO {
        private Integer id;
        private String nombre;
        private String descripcion;
        private Short estado;
    }
}
