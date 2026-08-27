package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Respuesta a la verificación de DPI (CU-00): indica si está registrado y si es
// paciente, para decidir si redirigir a login, a registro (FA03) o bloquear (FA04).
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerifyDpiResponseDTO {
    private boolean registrado;
    private boolean esPaciente;
    private String mensaje;
}