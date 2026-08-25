package com.example.demo.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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

    @Data
    public static class ConsultaResumenDTO {
        private String motivoVisita;
        private String hallazgosClinicos;
        private String diagnostico;
        private String planTratamiento;
        private boolean finalizada;
    }

    @Data
    public static class OrdenLaboratorioResumenDTO {
        private Integer id;
        private Short estado;
        private String estadoNombre;
        private BigDecimal montoTotal;
        private List<ExamenResultadoDTO> examenes;
    }

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

    @Data
    public static class RecetaResumenDTO {
        private Integer id;
        private String notas;
        private List<MedicamentoRecetaDTO> medicamentos;
    }

    @Data
    public static class MedicamentoRecetaDTO {
        private String nombreMedicamento;
        private String dosis;
        private String frecuencia;
        private String duracion;
        private String indicaciones;
    }
}