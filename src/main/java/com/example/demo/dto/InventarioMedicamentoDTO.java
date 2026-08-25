package com.example.demo.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

public class InventarioMedicamentoDTO {

    @Data
    public static class CreateDTO {
        @NotNull(message = "Debe seleccionar un medicamento.")
        private Integer medicamentoId;

        @NotNull(message = "Debe seleccionar una sucursal.")
        private Integer sucursalId;

        @NotNull(message = "El stock actual es obligatorio.")
        @Min(value = 0, message = "El stock actual no puede ser negativo.")
        private Integer stockActual;
    }

    @Data
    public static class ResponseDTO {
        private Integer id;
        private Integer medicamentoId;
        private String medicamentoNombre;
        private Integer sucursalId;
        private String sucursalNombre;
        private Integer stockActual;
        private Integer stockMinimo;
        private Boolean stockBajo;
        private Integer version;
    }

    @Data
    public static class LowStockAlertDTO {
        private Integer medicamentoId;
        private String medicamentoNombre;
        private Integer sucursalId;
        private String sucursalNombre;
        private Integer stockActual;
        private Integer stockMinimo;
        private String mensaje;
    }

    @Data
    public static class SummaryDTO {
        private Integer medicamentoId;
        private String medicamentoNombre;
        private Integer sucursalId;
        private String sucursalNombre;
        private String mesAnio; // formato "YYYY-MM"
        private Long totalEntradas;
        private Long totalSalidas;
        private Integer stockActual;
    }
}
