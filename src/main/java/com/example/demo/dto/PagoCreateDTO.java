package com.example.demo.dto;

import lombok.Data;

@Data
public class PagoCreateDTO {
    private Integer citaId;
    private String numeroTarjeta;   // 13-19 dígitos
    private String nombreTitular;
    private String vencimiento;     // MM/AA
    private String cvv;             // 3-4 dígitos
    private String idempotencyKey;  // UUID generado por el cliente
}