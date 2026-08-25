package com.example.demo.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegistroPacienteDTO {

    // RN-CU02-01
    @NotBlank(message = "El campo Nombre es obligatorio.")
    @Size(min = 10, max = 100, message = "El nombre debe contener entre 10 y 100 caracteres.")
    private String nombreCompleto;

    // RN-GLOBAL-001
    @NotBlank(message = "El campo DPI es obligatorio. Por favor, ingrese su número de DPI.")
    @Pattern(regexp = "^\\d{13}$", message = "El DPI debe contener exactamente 13 dígitos.")
    private String dpi;

    // RN-GLOBAL-002
    @NotBlank(message = "El campo NIT es obligatorio.")
    @Size(min = 8, max = 9, message = "El NIT debe contener entre 8 y 9 caracteres.")
    private String nit;

    // RN-CU02-02
    @NotBlank(message = "El número de teléfono es obligatorio.")
    @Pattern(regexp = "^\\d{8}$", message = "El número de teléfono debe contener exactamente 8 dígitos numéricos.")
    private String telefono;

    // RN-CU02-03 (opcional)
    @Size(min = 5, max = 50, message = "El número de seguro debe contener entre 5 y 50 caracteres.")
    private String numeroSeguro;

    // RN-CU02-04
    @NotBlank(message = "El campo Correo Electrónico es obligatorio.")
    @Email(message = "El formato del correo electrónico no es válido. Ejemplo: usuario@dominio.com")
    private String correoElectronico;

    // RN-CU02-05
    @NotBlank(message = "El campo Usuario es obligatorio.")
    @Size(min = 8, max = 9, message = "El usuario debe contener entre 8 y 9 caracteres.")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "El usuario debe contener únicamente caracteres alfanuméricos.")
    private String nombreUsuario;

    // RN-CU02-06
    @NotBlank(message = "El campo Contraseña es obligatorio.")
    @Size(min = 12, message = "La contraseña debe contener al menos 12 caracteres.")
    private String password;
}