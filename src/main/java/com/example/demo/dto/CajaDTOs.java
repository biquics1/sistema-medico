package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// Contenedor de todos los DTOs usados en el módulo de Caja (CU-06 Cobro de Consulta
// y CU-10 Cobro de Laboratorio en Caja): búsqueda de citas/órdenes pendientes de
// pago, solicitud de cobro y comprobante de pago generado.
public class CajaDTOs {

    // Cita mostrada al cajero antes de cobrar
    @Data
    @Builder
    @AllArgsConstructor
    public static class CitaCobroDTO {
        private Integer id;
        private String nombrePaciente;
        private String dpiPaciente;
        private String especialidad;
        private String sucursal;
        private String nombreMedico;
        private LocalDateTime fechaHora;
        private BigDecimal monto;
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class BusquedaCobroResultadoDTO {
        private boolean encontrada;
        private CitaCobroDTO cita;   // null si no se encontró
        private String mensaje;      // null si sí se encontró
    }

    // Lo que envía el cajero al confirmar el cobro
    @Data
    public static class CobrarRequestDTO {
        private String metodoPago;             // "EFECTIVO" | "VISA" | "MASTERCARD" | "DEBITO"
        private BigDecimal montoRecibido;       // obligatorio si metodoPago = EFECTIVO
        private String ultimosCuatroDigitos;    // obligatorio si es tarjeta
        private String uuidIdempotencia;        // opcional, evita doble clic = doble cobro
    }

    // Comprobante (RN-GLOBAL-005 / RN-CU06-03)
    @Data
    @Builder
    @AllArgsConstructor
    public static class ComprobantePagoDTO {
        private String mensaje;
        private String numeroTransaccion;
        private String nombrePaciente;
        private BigDecimal montoTotal;
        private BigDecimal montoRecibido;
        private BigDecimal cambioDevuelto;
        private String metodoPago;
        private String sucursal;
        private String detalleServicio;
        private LocalDateTime fechaPago;
    }

    // ---------------------------------------------------------------
    // Cobro de Laboratorio en Caja (precondición RN-CU09-01 de CU-09)
    // ---------------------------------------------------------------
    @Data
    @Builder
    @AllArgsConstructor
    public static class OrdenLabCobroDTO {
        private Integer id;
        private String nombrePaciente;
        private String dpiPaciente;
        private Integer cantidadExamenes;
        private LocalDateTime fechaCreacion;
        private BigDecimal montoTotal;
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class BusquedaCobroLabResultadoDTO {
        private boolean encontrada;
        private List<OrdenLabCobroDTO> ordenes;
        private String mensaje;
    }
}
