package com.example.demo.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UsuarioUpdateDTO {

    @NotBlank(message = "El campo Nombre es obligatorio.")
    @Size(min = 10, max = 100, message = "El nombre debe contener entre 10 y 100 caracteres.")
    private String nombreCompleto;

    @NotBlank(message = "El campo Correo Electrónico es obligatorio.")
    @Email(message = "El formato del correo electrónico no es válido.")
    private String correoElectronico;

    @NotBlank(message = "El campo Usuario es obligatorio.")
    @Size(min = 8, max = 9, message = "El usuario debe contener entre 8 y 9 caracteres.")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "El usuario debe contener únicamente caracteres alfanuméricos.")
    private String nombreUsuario;

    // Sin @Size aquí a propósito: en edición la contraseña es OPCIONAL
    // (RN-CU01-06 en edición). Si @Size(min=12) se dejara aquí, Bean
    // Validation la aplicaría también cuando el campo llega vacío ("")
    private String password;

    @Pattern(regexp = "^\\d{13}$", message = "El DPI debe contener exactamente 13 dígitos.")
    private String dpi;

    @Pattern(regexp = "^\\d{8}$", message = "El teléfono debe contener exactamente 8 dígitos.")
    private String telefono;

    @NotNull(message = "Debe seleccionar un rol para el usuario.")
    private Integer rolId;

    @Size(min = 8, max = 9, message = "El NIT debe contener entre 8 y 9 caracteres.")
    private String nit;

    @Size(min = 5, max = 50, message = "El número de seguro debe contener entre 5 y 50 caracteres.")
    private String numeroSeguro;

    private Integer sucursalId;

    private Integer especialidadId;

    private Short estado;
}