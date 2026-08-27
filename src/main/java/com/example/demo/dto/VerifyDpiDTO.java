package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

// Body para verificar si un DPI ya está registrado en el sistema (CU-00, portal
// web público antes de agendar cita).
@Data
public class VerifyDpiDTO {

    @NotBlank(message = "El campo DPI es obligatorio. Por favor, ingrese su número de DPI.")
    @Pattern(regexp = "^\\d{13}$", message = "El DPI debe contener exactamente 13 dígitos.")
    private String dpi;
}