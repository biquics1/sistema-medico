package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Respuesta tras un registro exitoso de paciente (CU-02): mensaje de confirmación
// y el nombre de usuario con el que ya puede iniciar sesión.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistroPacienteResponseDTO {
    private String mensaje;
    private String nombreUsuario;
}