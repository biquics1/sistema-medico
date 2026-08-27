package com.example.demo.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// Contenedor de DTOs para el portal del paciente ("Mis Citas"): resumen y detalle
// de citas propias, incluyendo consulta médica, orden de laboratorio y recetas
// asociadas (solo se exponen resultados de laboratorio ya publicados).
public class PacienteDTOs {

    // Fila de la lista "Mis Citas"
    @Data
    public static class MiCitaResumenDTO {
        private Integer id;
        private String especialidad;
        private String medico;
        private String sucursal;
        private LocalDateTime fechaHora;
        private String estado;
    }

    // Detalle completo de una cita: incluye consulta médica, orden de laboratorio y receta(s) si existen
    @Data
    public static class MiCitaDetalleDTO {
        private Integer id;
        private String especialidad;
        private String medico;
        private String sucursal;
        private LocalDateTime fechaHora;
        private String estado;
        private String motivoConsulta;
        private BigDecimal monto;

        // null si el médico aún no ha iniciado/registrado la consulta
        private ConsultaResumenDTO consulta;

        // null si no se generó orden de laboratorio para esta consulta
        private OrdenLaboratorioResumenDTO ordenLaboratorio;

        // vacío si no se generó receta para esta consulta
        private List<RecetaResumenDTO> recetas;
    }

    // Resumen de la consulta médica asociada a la cita (visible para el paciente).
    @Data
    public static class ConsultaResumenDTO {
        private String motivoVisita;
        private String hallazgosClinicos;
        private String diagnostico;
        private String planTratamiento;
        private boolean finalizada;
    }

    // Resumen de la orden de laboratorio asociada, con los exámenes y sus resultados publicados.
    @Data
    public static class OrdenLaboratorioResumenDTO {
        private Integer id;
        private Short estado;
        private String estadoNombre;
        private BigDecimal montoTotal;
        private List<ExamenResultadoDTO> examenes;
    }

    // Resultado de un examen visible para el paciente (solo si publicado = true).
    @Data
    public static class ExamenResultadoDTO {
        private String nombreExamen;
        private boolean publicado;
        // Los siguientes solo vienen informados si publicado = true (RN-CU09-02: resultados
        // no publicados no deben ser visibles para el paciente).
        private String valorResultado;
        private String unidad;
        private String rangoReferencia;
        private boolean fueraDeRango;
    }

    // Resumen de una receta médica asociada a la cita, con sus medicamentos.
    @Data
    public static class RecetaResumenDTO {
        private Integer id;
        private String notas;
        private List<MedicamentoRecetaDTO> medicamentos;
    }

    // Detalle de un medicamento recetado (dosis, frecuencia, duración, indicaciones).
    @Data
    public static class MedicamentoRecetaDTO {
        private String nombreMedicamento;
        private String dosis;
        private String frecuencia;
        private String duracion;
        private String indicaciones;
    }
}