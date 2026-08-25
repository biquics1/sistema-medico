package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequestDTO {

    @NotBlank(message = "El campo Usuario es obligatorio.")
    private String nombreUsuario;

    @NotBlank(message = "El campo Contraseña es obligatorio.")
    private String password;
}