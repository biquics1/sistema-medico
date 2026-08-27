package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// Contenedor de todos los DTOs del módulo de Gestión de Laboratorio (CU-09):
// listado y detalle de órdenes, exámenes dentro de una orden, y registro/publicación
// de resultados (publicación individual por examen, no masiva).
public class LaboratorioDTOs {

    // Paso 1 FB: fila de la tabla "Órdenes de Laboratorio"
    @Data
    @Builder
    @AllArgsConstructor
    public static class OrdenListaDTO {
        private Integer id;
        private String nombrePaciente;
        private String dpiPaciente;
        private String nombreMedico;
        private Short estadoCodigo;      // 0..3
        private String estado;           // texto: Pendiente/En proceso/Completada/Cancelada
        private boolean esExterna;
        private Integer cantidadExamenes;
        private BigDecimal montoTotal;
        private LocalDateTime creadoEn;
    }

    // Detalle de un examen dentro de la orden
    @Data
    @Builder
    @AllArgsConstructor
    public static class ExamenOrdenDTO {
        private Integer id; // id de detalle_orden_laboratorio
        private Integer examenId;
        private String nombreExamen;
        private String codigoExamen;
        private BigDecimal precioUnitario;
        private String rangoReferencia;
        private String valorResultado;
        private String unidad;
        private boolean fueraDeRango;
        private String notasResultado;
        private boolean publicado;
        private LocalDateTime fechaResultado;
    }

    // Paso 3 FB: detalle completo de la orden
    @Data
    @Builder
    @AllArgsConstructor
    public static class OrdenDetalleDTO {
        private Integer id;
        private String nombrePaciente;
        private String dpiPaciente;
        private String nombreMedico;
        private Short estadoCodigo;
        private String estado;
        private boolean esExterna;
        private BigDecimal montoTotal;
        private String notas;
        private LocalDateTime creadoEn;
        private List<ExamenOrdenDTO> examenes;
    }

    // Paso 9-10 FB: registrar resultado de un examen (RN-CU09-02)
    @Data
    public static class ResultadoRequestDTO {
        private String valorResultado;
        private String unidad;
        private boolean fueraDeRango;
        private String notasResultado;
        private LocalDateTime fechaResultado; // opcional; si es null se usa now()
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class ResultadoResponseDTO {
        private String mensaje;
        private ExamenOrdenDTO examen;
    }

    // Paso 11-12 FB: publicar resultado individual
    @Data
    @Builder
    @AllArgsConstructor
    public static class PublicarResponseDTO {
        private String mensaje;
        private ExamenOrdenDTO examen;
        private boolean ordenCompletada; // true si con esta publicación se completó toda la orden
    }
}
