package com.example.demo.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

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
}