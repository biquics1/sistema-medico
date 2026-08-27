package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

// Body del login de usuarios internos y pacientes (CU-00 / CU-01 / RN-GLOBAL-007).
@Data
public class LoginRequestDTO {

    @NotBlank(message = "El campo Usuario es obligatorio.")
    private String nombreUsuario;

    @NotBlank(message = "El campo Contraseña es obligatorio.")
    private String password;
}