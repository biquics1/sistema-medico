package com.example.demo.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;

public class EventoAgendaDTO {

    @Data
    public static class CreateDTO {
        @NotBlank(message = "El título es obligatorio.")
        @Size(min = 5, max = 200, message = "El título debe contener entre 5 y 200 caracteres.")
        private String titulo;

        @Size(max = 2000, message = "La descripción no puede exceder los 2000 caracteres.")
        private String descripcion;

        // 0=Reunión, 1=Descanso, 2=Capacitación, 3=Personal, 4=Otro
        @NotNull(message = "Debe seleccionar un tipo de evento.")
        @Min(value = 0, message = "Tipo de evento inválido.")
        @Max(value = 4, message = "Tipo de evento inválido.")
        private Short tipoEvento;

        @NotNull(message = "La fecha de inicio es obligatoria.")
        private LocalDateTime fechaInicio;

        @NotNull(message = "La fecha de fin es obligatoria.")
        private LocalDateTime fechaFin;

        private Boolean todoElDia;
    }

    @Data
    public static class ResponseDTO {
        private Integer id;
        private String titulo;
        private String descripcion;
        private Short tipoEvento;
        private String tipoEventoNombre;
        private LocalDateTime fechaInicio;
        private LocalDateTime fechaFin;
        private Boolean todoElDia;
        private Integer medicoId;
    }
}