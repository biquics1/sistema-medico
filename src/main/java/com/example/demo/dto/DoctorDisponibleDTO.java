package com.example.demo.dto;

import lombok.Data;
import lombok.AllArgsConstructor;

// DTO simple (id + nombre) para listar médicos disponibles al agendar una cita
// (CU-03, paso 3 del wizard) o al reasignar médico (CU-05, FA07).
@Data
@AllArgsConstructor
public class DoctorDisponibleDTO {
    private Integer id;
    private String nombreCompleto;
}