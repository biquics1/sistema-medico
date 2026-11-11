package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

// DTOs del catálogo "Marcas de Tarjeta" (CU-14) con el conteo de usos.
public class TarjetaMarcaDTO {

    @Data
    public static class CreateDTO {
        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 30, message = "El nombre no puede exceder los 30 caracteres.")
        private String nombre;

        @Size(max = 200, message = "La descripción no puede exceder los 200 caracteres.")
        private String descripcion;

        @NotBlank(message = "El patrón del número de tarjeta es obligatorio.")
        @Size(max = 200, message = "El patrón no puede exceder los 200 caracteres.")
        private String patron;

        @NotNull(message = "Debe seleccionar un estado.")
        private Short estado;
    }

    // Fila del historial de tarjetas usadas (solo lectura).
    @Data
    public static class RegistradaDTO {
        private Integer id;
        private String marca;
        private String tarjeta;        // enmascarada: **** **** **** 1234
        private String nombreTitular;
        private Integer vecesUsada;
        private java.time.LocalDateTime primerUso;
        private java.time.LocalDateTime ultimoUso;
    }

    @Data
    public static class ResponseDTO {
        private Integer id;
        private String nombre;
        private String descripcion;
        private String patron;
        private Long totalUsos;
        private Short estado;
    }
}
