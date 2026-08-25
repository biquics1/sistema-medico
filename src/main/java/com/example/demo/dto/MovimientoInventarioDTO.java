package com.example.demo.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MovimientoInventarioDTO {

    @Data
    public static class CreateDTO {
        @NotNull(message = "Debe seleccionar un medicamento.")
        private Integer medicamentoId;

        @NotNull(message = "Debe seleccionar una sucursal.")
        private Integer sucursalId;

        // Solo 0-5 permitido en creación manual (6=Despacho es automático)
        @NotNull(message = "Debe seleccionar el tipo de movimiento.")
        @Min(value = 0, message = "Tipo de movimiento inválido.")
        @Max(value = 5, message = "Tipo de movimiento inválido para creación manual.")
        private Short tipoMovimiento;

        @NotNull(message = "La cantidad debe ser un número entero positivo.")
        @Positive(message = "La cantidad debe ser un número entero positivo.")
        private Integer cantidad;

        // Obligatorio solo para Compra (0), > 0, máx 2 decimales
        private BigDecimal costoUnitario;

        private String numeroReferencia;

        // Obligatorio para Devolución(1), Reclamo(3), Ajuste+(4), Ajuste-(5); opcional en Compra(0); no aplica en Venta(2)
        private String motivo;
    }

    @Data
    public static class ResponseDTO {
        private Integer id;
        private Integer medicamentoId;
        private String medicamentoNombre;
        private Integer sucursalId;
        private String sucursalNombre;
        private Short tipoMovimiento;
        private String tipoMovimientoNombre;
        private Integer cantidad;
        private Integer stockAnterior;
        private Integer stockNuevo;
        private BigDecimal costoUnitario;
        private String numeroReferencia;
        private String motivo;
        private Integer usuarioId;
        private String usuarioNombre;
        private Boolean activo;
        private LocalDateTime creadoEn;
    }
}
