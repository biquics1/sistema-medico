package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

// Contenedor de DTOs del Agendamiento de Cita de Seguimiento (CU-11): banner
// precargado desde la consulta padre y confirmación del nuevo agendamiento.
public class SeguimientoDTOs {

    // Paso 2-3 FB: banner con los datos precargados desde la consulta padre
    @Data
    @Builder
    @AllArgsConstructor
    public static class ContextoSeguimientoDTO {
        private Integer consultaId;
        private Integer citaOrigenId;
        private Integer pacienteId;
        private String nombrePaciente;
        private Integer medicoId;
        private String nombreMedico;
        private Integer sucursalId;
        private String nombreSucursal;
        private Integer especialidadId;
        private String nombreEspecialidad;
    }

    // Paso 4-6 FB: lo que envía el médico al confirmar el agendamiento
    @Data
    public static class CrearSeguimientoRequestDTO {
        private String tipoSeguimiento;      // MONITOREO_TRATAMIENTO | REVISION_RESULTADOS (RN-CU11-01)
        private LocalDateTime fechaHora;     // RN-CU11-02
        private String motivoSeguimiento;    // "Observaciones" (RN-CU11-03)
    }

    // Paso 8 FB: toast de éxito
    @Data
    @Builder
    @AllArgsConstructor
    public static class SeguimientoResponseDTO {
        private String mensaje;
        private Integer citaSeguimientoId;
        private Integer citaNuevaId;
        private String tipoSeguimiento;
        private LocalDateTime fechaHora;
    }
}
