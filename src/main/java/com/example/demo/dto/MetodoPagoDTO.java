package com.example.demo.dto;

import lombok.Data;

// Body del nuevo paso "método de pago" (wizard paso 6, tras confirmar la
// cita): el paciente elige "CAJA" (paga físicamente después, sin
// vencimiento automático) o "LINEA" (pago con tarjeta, ventana de
// app.reserva.online.minutos antes de cancelarse automáticamente).
@Data
public class MetodoPagoDTO {
    private String metodoPago; // "CAJA" o "LINEA"
}