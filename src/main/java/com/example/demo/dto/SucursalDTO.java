package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

public class SucursalDTO {

    // Body para crear/actualizar una sucursal (RN-CU15-04: teléfono opcional pero
    // si se ingresa debe tener exactamente 8 dígitos).
    @Data
    public static class CreateDTO {
        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 100, message = "El nombre no puede exceder los 100 caracteres.")
        private String nombre;

        // RN-CU15-04: opcional, pero si se ingresa debe tener exactamente 8 dígitos.
        // @Pattern no falla en null, así que queda opcional automáticamente.
        @Pattern(regexp = "^\\d{8}$", message = "El teléfono debe tener exactamente 8 dígitos.")
        private String telefono;

        // RN-CU15-04: opcional, máximo 500 caracteres.
        @Size(max = 500, message = "La dirección no puede exceder los 500 caracteres.")
        private String direccion;

        @Size(max = 250, message = "La descripción no puede exceder los 250 caracteres.")
        private String descripcion;

        @NotNull(message = "Debe seleccionar un estado.")
        private Short estado;
    }

    // Datos de la sucursal para el listado y para poblar dropdowns de sede.
    @Data
    public static class ResponseDTO {
        private Integer id;
        private String nombre;
        private String telefono;
        private String direccion;
        private String descripcion;
        private Short estado;
    }
}
