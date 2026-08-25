package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

public class RecepcionDTOs {

    // FA03: paciente no existe -> frontend muestra solo "Registrar Paciente"
    // FA04: paciente existe pero sin citas -> frontend muestra solo "Nueva Cita (Walk-in)"
    @Data
    @Builder
    @AllArgsConstructor
    public static class BusquedaResultadoDTO {
        private boolean citaEncontrada;
        private boolean pacienteExiste;
        private CitaRecepcionDTO cita;      // null si no hay cita
        private String mensaje;             // null si sí se encontró cita
        private String accionSugerida;      // "REGISTRAR_PACIENTE" | "NUEVA_CITA_WALKIN" | null
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class RegistrarLlegadaResponseDTO {
        private String mensaje;
        private CitaRecepcionDTO cita;
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class MedicoDisponibleDTO {
        private Integer id;
        private String nombreCompleto;
    }

    @Data
    public static class ReasignarMedicoRequestDTO {
        private Integer idMedicoNuevo;
        private String motivo;               // opcional
    }
}