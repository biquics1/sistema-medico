package com.example.demo.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

public class TareaMedicoDTO {

    @Data
    public static class CreateDTO {
        @NotBlank(message = "El título es obligatorio.")
        @Size(min = 5, max = 200, message = "El título debe contener entre 5 y 200 caracteres.")
        private String titulo;

        @Size(max = 1000, message = "La descripción no puede exceder los 1000 caracteres.")
        private String descripcion;

        // 0=Baja, 1=Normal, 2=Alta
        @NotNull(message = "Debe seleccionar una prioridad.")
        @Min(value = 0, message = "Prioridad inválida.")
        @Max(value = 2, message = "Prioridad inválida.")
        private Short prioridad;

        // Opcional; si se ingresa, se valida que sea fecha futura en el service.
        private LocalDate fechaLimite;
    }

    @Data
    public static class ResponseDTO {
        private Integer id;
        private String titulo;
        private String descripcion;
        private Short prioridad;
        private String prioridadNombre;
        private LocalDate fechaLimite;
        private Boolean completada;
        private Integer medicoId;
    }
}