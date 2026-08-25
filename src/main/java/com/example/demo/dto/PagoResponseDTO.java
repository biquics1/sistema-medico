package com.example.demo.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class PagoResponseDTO {
    private Integer citaId;
    private String numeroTransaccion;
    private BigDecimal monto;
    private String estadoCita;
    private String mensaje;
}