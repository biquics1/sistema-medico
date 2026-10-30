package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// Contenedor de todos los DTOs del módulo de Consulta Médica (CU-08): panel del
// médico agrupado por estado, formulario de consulta, catálogo CIE-10, generación
// de órdenes de laboratorio y recetas, y acciones de cierre de la cita.
// ============================================================================
// CU-08 - DTOs (8_CU_Consulta_Medica.docx). Este archivo NO valida: no usa @NotBlank/@Size.
// Todas las validaciones (>>> VALIDACION) están en ConsultaMedicaService.
//   PanelMedicoDTO / CitaPanelDTO ........ FB paso 1
//   IniciarConsultaResponseDTO ........... FB paso 2
//   ConsultaContextoDTO .................. FB pasos 3-4 (formulario precargado)
//   GuardarConsulta Request/Response ..... FB pasos 5-10 (y FA05)
//   Cie10DTO ............................. FB paso 7
//   Examen*/OrdenLaboratorio* ............ FA01
//   Medicamento*/Receta* ................. FA04
//   AccionCitaResponseDTO ................ FA06 y FB pasos 11-12
// ============================================================================
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
        private Short vecesLlamado; // CU-08: veces que se ha llamado al paciente a consulta (maximo 3)
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

    // Body para guardar la consulta (borrador o cierre) - FB pasos 5 a 9:
    //   motivoVisita .... paso 5  - obligatorio siempre (RN-CU08-02)
    //   hallazgosClinicos paso 6  - obligatorio al finalizar (RN-CU08-02)
    //   cie10Id ......... paso 7  - opcional; si viene debe existir en el catálogo
    //   diagnostico ..... paso 8  - obligatorio al finalizar, 10-5000 caracteres (RN-CU08-01, FA05)
    //   planTratamiento . paso 8  - obligatorio al finalizar (RN-CU08-02)
    //   notasAdicionales  opcional
    //   estadoConsulta .. paso 9  - "EN_CURSO" | "FINALIZADA" (FINALIZADA activa las validaciones de cierre)
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
        // >>> FA01: obligatorio con al menos 1 examen (se valida en el service). esExterna = casilla 'Orden Externa' (CU-09 FA01); notas = observaciones del médico.
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
        // >>> RN-CU08-03 (se valida en el service): medicamentoId, dosis, frecuencia y duracion obligatorios; indicaciones opcional.
        private Integer medicamentoId;
        private String dosis;
        private String frecuencia;
        private String duracion;
        private String indicaciones;
    }

    @Data
    public static class RecetaRequestDTO {
        // >>> FA04: al menos 1 detalle (se valida en el service).
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