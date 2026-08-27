// ============================================================
// ENTIDAD/ENUM: MetodoPago
// Enum simple (no es tabla) que representa los métodos de pago
// aceptados por el sistema. Se usa como valor en columnas de
// tipo texto (ej. Pago.metodoPago) [RN-GLOBAL-004].
// ============================================================
package com.example.demo.modelo;

public enum MetodoPago {
    EFECTIVO,       // Pago en efectivo (Quetzales)
    VISA,           // Tarjeta de crédito Visa
    MASTERCARD,     // Tarjeta de crédito Mastercard
    DEBITO,         // Tarjeta de débito
    PASARELA_LINEA // reservado para CU-04 (pago en línea), no se usa en CU-06
}
