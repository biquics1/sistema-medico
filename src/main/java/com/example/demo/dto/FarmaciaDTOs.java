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
    // Carrito de compra (mezcla ítems con receta y sin receta) + pago
    // ---------------------------------------------------------------
    // Un ítem individual del carrito de despacho (puede venir de una receta o ser venta libre).
    @Data
    public static class CarritoItemDTO {
        private String origen;                  // "RECETA" o "LIBRE"
        private Integer recetaId;               // obligatorio si origen = RECETA
        private Integer medicamentoId;
        private Integer cantidad;
        private boolean sustituido;             // solo aplica si origen = RECETA (FA02)
        private Integer medicamentoSustitutoId; // obligatorio si sustituido = true
        private String razonSustitucion;        // obligatorio si sustituido = true
    }

    // Body que envía farmacia al confirmar todo el carrito (paso "Confirmar Despacho").
    @Data
    public static class ConfirmarCarritoRequestDTO {
        private Integer idSucursal;
        private Integer idPaciente;       // opcional, solo para ítems de venta libre (comprobante)
        private String metodoPago;        // EFECTIVO, VISA, MASTERCARD, DEBITO
        private BigDecimal montoRecibido; // obligatorio si metodoPago = EFECTIVO
        private String ultimosCuatroDigitos; // obligatorio si es tarjeta
        private String uuidIdempotencia;  // opcional, evita cobros duplicados
        private List<CarritoItemDTO> items;
    }

    // Ítem ya despachado, para el resumen final del comprobante.
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
        private Integer recetaId; // null si fue venta libre
    }

    // Resumen del carrito ya pagado/confirmado
    @Data
    @Builder
    @AllArgsConstructor
    public static class ConfirmarCarritoResponseDTO {
        private String mensaje;
        private String numeroTransaccion;
        private BigDecimal montoTotal;
        private BigDecimal montoRecibido;
        private BigDecimal cambioDevuelto;
        private String metodoPago;
        private List<ItemDespachadoDTO> itemsDespachados;
        private List<String> alertas; // sin inventario / stock bajo / stock mínimo alcanzado / sustituciones
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
