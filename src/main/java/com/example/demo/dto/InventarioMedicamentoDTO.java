package com.example.demo.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

// Contenedor de DTOs del catálogo "Inventario de Medicamentos" (CU-15): stock
// inicial por sucursal, alertas de stock bajo y resumen mensual de movimientos.
public class InventarioMedicamentoDTO {

    // Body para registrar el stock inicial de un medicamento en una sucursal.
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

    // Datos del inventario de un medicamento en una sucursal, con indicador de stock bajo.
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

    // Alerta de stock mínimo alcanzado (RN-CU10-03), mostrada en el panel de inventario.
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

    // Resumen mensual de entradas/salidas de un medicamento en una sucursal
    // (usado en la bitácora embebida de MedicineInventoryPage).
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
