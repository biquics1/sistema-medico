package com.example.demo.dto;

import lombok.Data;
import java.time.LocalDateTime;

// Body para crear una cita (CU-03 Agendar Citas, wizard de 5 pasos, y también
// usado para walk-ins desde Recepción y para citas de seguimiento).
@Data
public class CitaCreateDTO {
    private Integer pacienteId;
    private Integer sucursalId;
    private Integer especialidadId;
    private Integer medicoId;
    private LocalDateTime fechaHora;
    private String motivoConsulta;
}