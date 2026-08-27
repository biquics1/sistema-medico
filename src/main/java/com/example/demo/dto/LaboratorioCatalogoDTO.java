package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

// Nombre distinto a "LaboratorioDTOs" (con "s") a propósito: ese ya existe y es
// para las órdenes de laboratorio (CU-09). Este es solo el catálogo de laboratorios.
public class LaboratorioCatalogoDTO {

    // Body para crear/actualizar un laboratorio del catálogo.
    @Data
    public static class CreateDTO {
        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 200, message = "El nombre no puede exceder los 200 caracteres.")
        private String nombre;

        @Size(max = 500, message = "La descripción no puede exceder los 500 caracteres.")
        private String descripcion;

        @NotNull(message = "Debe seleccionar un estado.")
        private Short estado;
    }

    // Datos del laboratorio para el listado.
    @Data
    public static class ResponseDTO {
        private Integer id;
        private String nombre;
        private String descripcion;
        private Short estado;
    }
}
