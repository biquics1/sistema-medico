package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class EnfermeriaDTOs {

    // Tarjeta de paciente en la cola (tanto "Paciente Presente" como "Signos Vitales")
    @Data
    @Builder
    @AllArgsConstructor
    public static class PacienteColaDTO {
        private Integer id;              // = número de cita
        private String nombrePaciente;
        private String especialidad;
        private String sucursal;
        private String estado;
        private boolean esEmergencia;
        private LocalDateTime fechaHora;
        private LocalDateTime horaLlegada;
    }

    // Respuesta del listado: separa "Paciente Presente" de "Signos Vitales" (en proceso)
    @Data
    @Builder
    @AllArgsConstructor
    public static class ColaEnfermeriaDTO {
        private List<PacienteColaDTO> presentes;   // esperando ser llamados
        private List<PacienteColaDTO> enProceso;   // ya llamados, en toma de signos
    }

    // Paso 1 FB: respuesta al "Llamar y Tomar Signos" (incluye el texto para el TTS)
    @Data
    @Builder
    @AllArgsConstructor
    public static class LlamarPacienteResponseDTO {
        private String mensajeAnuncio;   // texto a leer por síntesis de voz
        private PacienteColaDTO cita;
    }

    // Body que envía enfermería al registrar los signos vitales
    @Data
    public static class RegistrarSignosVitalesRequestDTO {
        private Integer presionSistolica;
        private Integer presionDiastolica;
        private BigDecimal temperatura;
        private BigDecimal peso;
        private BigDecimal talla;
        private Integer frecuenciaCardiaca;
        private boolean esEmergencia;
    }

    // Paso 9-11 FB: confirmación + alertas clínicas en tiempo real (RN-CU07-06)
    @Data
    @Builder
    @AllArgsConstructor
    public static class SignosVitalesResponseDTO {
        private String mensaje;
        private List<String> alertasClinicas; // vacío si todo está en rango normal
        private PacienteColaDTO cita;
    }
}
