package com.example.demo.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// Representación general de una cita para listados/consultas (ej. CU-03, CU-16),
// con los nombres de paciente, médico, sucursal y especialidad ya resueltos.
@Data
@AllArgsConstructor
public class CitaResponseDTO {
    private Integer id;
    private String paciente;
    private String medico;
    private String sucursal;
    private String especialidad;
    private String estado;
    private LocalDateTime fechaHora;
    private String motivoConsulta;
    private BigDecimal monto;
    private LocalDateTime expiraEn;
    private LocalDateTime sesionPagoExpiraEn;
}