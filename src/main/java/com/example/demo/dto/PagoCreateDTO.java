package com.example.demo.dto;

import lombok.Data;

// Body del pago en línea con tarjeta (CU-04): datos de la tarjeta más la
// idempotencyKey para evitar cobros duplicados por doble clic (RNF-016).
@Data
public class PagoCreateDTO {
    private Integer citaId;
    private String numeroTarjeta;   // 13-19 dígitos
    private String nombreTitular;
    private String vencimiento;     // MM/AA
    private String cvv;             // 3-4 dígitos
    private String idempotencyKey;  // UUID generado por el cliente
}