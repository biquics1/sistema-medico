package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

// Contenedor de DTOs del catálogo "Roles" (CU-01 / CU-15).
public class RolDTO {

    // Body para crear/actualizar un rol.
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

    // Datos del rol para el listado / dropdown de asignación de usuarios.
    @Data
    public static class ResponseDTO {
        private Integer id;
        private String nombre;
        private String descripcion;
        private Short estado;
    }
}
