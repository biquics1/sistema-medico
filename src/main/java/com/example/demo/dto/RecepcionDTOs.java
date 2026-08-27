package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

// Contenedor de DTOs del módulo de Recepción y Verificación de Cita (CU-05):
// búsqueda de cita/paciente, registro de llegada y reasignación de médico.
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

    // Confirmación de que se registró la llegada del paciente (FB paso 7 de CU-05).
    @Data
    @Builder
    @AllArgsConstructor
    public static class RegistrarLlegadaResponseDTO {
        private String mensaje;
        private CitaRecepcionDTO cita;
    }

    // Médico disponible para reasignación (misma sede y especialidad, FA07).
    @Data
    @Builder
    @AllArgsConstructor
    public static class MedicoDisponibleDTO {
        private Integer id;
        private String nombreCompleto;
    }

    // Body para confirmar la reasignación de médico de una cita (FA07).
    @Data
    public static class ReasignarMedicoRequestDTO {
        private Integer idMedicoNuevo;
        private String motivo;               // opcional
    }
}