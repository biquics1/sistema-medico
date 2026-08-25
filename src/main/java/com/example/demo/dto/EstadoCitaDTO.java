package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

public class EstadoCitaDTO {

    @Data
    public static class CreateDTO {
        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 50, message = "El nombre no puede exceder los 50 caracteres.")
        private String nombre;

        @Size(max = 200, message = "La descripción no puede exceder los 200 caracteres.")
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
