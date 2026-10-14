package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// Contenedor de todos los DTOs del módulo de Farmacia (CU-10 Despacho de
// Medicamentos): búsqueda de recetas vigentes, carrito de despacho (recetados +
// venta libre), confirmación de despacho con cobro integrado, y gestión de
// catálogo/stock de medicamentos por el Administrador.
public class FarmaciaDTOs {

    // ---------------------------------------------------------------
    // Búsqueda de recetas activas (state = 1)
    // ---------------------------------------------------------------
    @Data
    @Builder
    @AllArgsConstructor
    public static class RecetaBusquedaDTO {
        private Integer id;
        private Integer consultaId;
        private LocalDateTime fechaEmision;
        private boolean vigente;          // false si ya pasaron más de 7 días (RN-CU10-01)
        private long diasTranscurridos;
        private String notas;
    }

    // Cada medicamento recetado, con disponibilidad de inventario
    @Data
    @Builder
    @AllArgsConstructor
    public static class ItemRecetaDTO {
        private Integer medicamentoId;
        private String nombreMedicamento;
        private String dosis;
        private String frecuencia;
        private String duracion;
        private String indicaciones;
        private BigDecimal precioUnitario;
        private Integer stockDisponible;   // null si no hay registro de inventario (FA01)
        private Integer stockMinimo;
        private boolean stockBajo;
    }

    // Detalle completo de la receta, para armar el carrito
    @Data
    @Builder
    @AllArgsConstructor
    public static class RecetaDetalleDTO {
        private Integer id;
        private Integer consultaId;
        private String nombrePaciente;
        private LocalDateTime fechaEmision;
        private boolean vigente;
        private long diasTranscurridos;
        private List<ItemRecetaDTO> items;
        private BigDecimal montoTotalEstimado;
    }

    // ---------------------------------------------------------------
    // Catálogo/búsqueda de medicamentos (venta libre, sin receta)
    // ---------------------------------------------------------------
    @Data
    @Builder
    @AllArgsConstructor
    public static class MedicamentoCatalogoDTO {
        private Integer id;
        private String nombre;
        private String descripcion;
        private BigDecimal precio;
        private String unidad;
        private boolean esControlado;
        private Integer stockDisponible;   // null si no hay registro de inventario en la sucursal
        private Integer stockMinimo;
        private boolean stockBajo;
    }

    // ---------------------------------------------------------------
    // Despacho de una receta (pasos 7-13 del flujo básico).
    // El cobro físico se hace FUERA del sistema: no viaja método de pago.
    // ---------------------------------------------------------------
    // Un ítem de la receta que el personal desea despachar.
    @Data
    public static class DespachoItemDTO {
        private Integer medicamentoId;          // medicamento tal como fue recetado
        private Integer cantidad;
        private boolean sustituido;             // FA02
        private Integer medicamentoSustitutoId; // obligatorio si sustituido = true
        private String razonSustitucion;        // obligatorio si sustituido = true
    }

    // Body de "Confirmar Despacho".
    @Data
    public static class ConfirmarDespachoRequestDTO {
        private Integer idSucursal;
        private List<DespachoItemDTO> items;
    }

    // Ítem ya despachado, para el resumen final (paso 12).
    @Data
    @Builder
    @AllArgsConstructor
    public static class ItemDespachadoDTO {
        private String nombreMedicamento;
        private Integer cantidad;
        private BigDecimal precioUnitario;
        private BigDecimal subtotal;
        private boolean sustituido;
        private String nombreMedicamentoSustituto;
    }

    // Resumen del despacho (pasos 12-13).
    @Data
    @Builder
    @AllArgsConstructor
    public static class ConfirmarDespachoResponseDTO {
        private String mensaje;
        private Integer despachoId;
        private BigDecimal montoTotal;
        private List<ItemDespachadoDTO> itemsDespachados;
        private List<String> alertas; // sin inventario / stock bajo / sustituciones / stock mínimo alcanzado (FA04)
    }

    // ---------------------------------------------------------------
    // FA03: el paciente no desea adquirir los medicamentos de una receta
    // ---------------------------------------------------------------
    @Data
    @Builder
    @AllArgsConstructor
    public static class CancelarDespachoResponseDTO {
        private String mensaje;
    }

    // ---------------------------------------------------------------
    // NUEVO: alta de medicamentos al catálogo (Administrador)
    // ---------------------------------------------------------------
    @Data
    public static class CrearMedicamentoRequestDTO {
        private String nombre;
        private String descripcion;
        private BigDecimal precio;
        private String unidad;
        private boolean esControlado;
        private Integer stockMinimo; // opcional
    }

    // ---------------------------------------------------------------
    // NUEVO: ajuste/carga de stock (Administrador), mismo patrón que CU-13 Bitácora
    // ---------------------------------------------------------------
    @Data
    public static class AjustarStockRequestDTO {
        private Integer medicamentoId;
        private Integer sucursalId;
        private String tipoMovimiento;  // COMPRA, AJUSTE_POSITIVO, AJUSTE_NEGATIVO
        private Integer cantidad;
        private BigDecimal costoUnitario; // obligatorio si tipoMovimiento = COMPRA
        private String motivo;            // obligatorio si tipoMovimiento = AJUSTE_POSITIVO / AJUSTE_NEGATIVO
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class AjustarStockResponseDTO {
        private String mensaje;
        private Integer stockAnterior;
        private Integer stockNuevo;
    }
}