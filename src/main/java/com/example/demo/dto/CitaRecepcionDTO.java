package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public class CitaRecepcionDTO {
    private Integer id;
    private String nombrePaciente;
    private String dpiPaciente;
    private String estado;          // nombre del estado_cita, ej. "Confirmada"
    private boolean esEmergencia;
    private String especialidad;
    private String sucursal;
    private Integer sucursalId;
    private Integer especialidadId;
    private LocalDateTime fechaHora;
    private String motivoConsulta;
    private LocalDateTime horaLlegada;
    private String nombreMedico;
    private Integer medicoId;
}