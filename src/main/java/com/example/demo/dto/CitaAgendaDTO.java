package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

// Representación liviana de una cita para pintarla en el calendario de Agenda Médica (CU-14).
// No reemplaza a CitaResponseDTO; es solo lo que necesita el componente de calendario.
@Data
@AllArgsConstructor
public class CitaAgendaDTO {
    private Integer id;
    private String pacienteNombre;
    private String especialidad;
    private String estado;
    private LocalDateTime fechaHora;
    private Boolean esEmergencia;
    private String motivoConsulta;
}