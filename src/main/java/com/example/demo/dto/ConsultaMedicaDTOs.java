package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class ConsultaMedicaDTOs {

    // Tarjeta de cita en el panel del médico
    @Data
    @Builder
    @AllArgsConstructor
    public static class CitaPanelDTO {
        private Integer id;
        private String nombrePaciente;
        private String especialidad;
        private LocalDateTime fechaHora;
        private String estado;
        private boolean esEmergencia;
    }

    // Paso 1 FB: panel agrupado en 3 secciones
    @Data
    @Builder
    @AllArgsConstructor
    public static class PanelMedicoDTO {
        private List<CitaPanelDTO> enEspera;
        private List<CitaPanelDTO> enConsulta;
        private List<CitaPanelDTO> evaluados;
    }

    // Paso 2 FB: respuesta de "Iniciar Consulta" (incluye texto para TTS)
    @Data
    @Builder
    @AllArgsConstructor
    public static class IniciarConsultaResponseDTO {
        private String mensajeAnuncio;
        private CitaPanelDTO cita;
    }

    // Paso 3 FB: contexto + datos ya guardados (si el médico reabre el formulario)
    @Data
    @Builder
    @AllArgsConstructor
    public static class ConsultaContextoDTO {
        private Integer citaId;
        private Integer consultaId; // null si aún no se ha guardado nada
        private String nombrePaciente;
        private String motivoVisita;
        private String hallazgosClinicos;
        private Integer cie10Id;
        private String cie10Codigo;
        private String diagnostico;
        private String planTratamiento;
        private String notasAdicionales;
        private boolean finalizada;
    }

    // Body para guardar la consulta (borrador o cierre)
    @Data
    public static class GuardarConsultaRequestDTO {
        private String motivoVisita;
        private String hallazgosClinicos;
        private Integer cie10Id;
        private String diagnostico;
        private String planTratamiento;
        private String notasAdicionales;
        private String estadoConsulta; // "EN_CURSO" | "FINALIZADA"
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class GuardarConsultaResponseDTO {
        private String mensaje;
        private Integer consultaId;
        private boolean finalizada;
        private String estadoCita;
    }

    // Catálogo CIE-10 (autocompletado)
    @Data
    @Builder
    @AllArgsConstructor
    public static class Cie10DTO {
        private Integer id;
        private String codigo;
        private String descripcion;
    }

    // Catálogo de exámenes de laboratorio (FA01)
    @Data
    @Builder
    @AllArgsConstructor
    public static class ExamenCatalogoDTO {
        private Integer id;
        private String codigo;
        private String nombre;
        private BigDecimal precio;
    }

    @Data
    public static class OrdenLaboratorioRequestDTO {
        private List<Integer> examenIds;
        private boolean esExterna;
        private String notas;
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class OrdenLaboratorioResponseDTO {
        private String mensaje;
        private Integer numeroOrden;
        private List<String> examenes;
        private BigDecimal montoTotal;
    }

    // Catálogo de medicamentos (FA04)
    @Data
    @Builder
    @AllArgsConstructor
    public static class MedicamentoCatalogoDTO {
        private Integer id;
        private String nombre;
        private String unidad;
        private boolean esControlado;
    }

    @Data
    public static class DetalleRecetaRequestDTO {
        private Integer medicamentoId;
        private String dosis;
        private String frecuencia;
        private String duracion;
        private String indicaciones;
    }

    @Data
    public static class RecetaRequestDTO {
        private List<DetalleRecetaRequestDTO> detalles;
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class RecetaResponseDTO {
        private String mensaje;
        private Integer recetaId;
        private List<String> medicamentos;
    }

    // Respuesta genérica para acciones simples sobre una cita
    // (FA06 "No Asistió" y el cierre "Finalizar Atención")
    @Data
    @Builder
    @AllArgsConstructor
    public static class AccionCitaResponseDTO {
        private String mensaje;
        private CitaPanelDTO cita;
    }
}
