package com.example.demo.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CitaCreateDTO {
    private Integer pacienteId;
    private Integer sucursalId;
    private Integer especialidadId;
    private Integer medicoId;
    private LocalDateTime fechaHora;
    private String motivoConsulta;
}