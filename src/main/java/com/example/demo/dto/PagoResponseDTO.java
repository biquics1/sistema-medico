package com.example.demo.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

// Respuesta tras procesar un pago en línea (CU-04): confirma el número de
// transacción y el nuevo estado de la cita ("Pagada").
@Data
@AllArgsConstructor
public class PagoResponseDTO {
    private Integer citaId;
    private String numeroTransaccion;
    private BigDecimal monto;
    private String estadoCita;
    private String mensaje;
}