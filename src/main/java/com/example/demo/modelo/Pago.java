package com.example.demo.modelo;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pago")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "cita_id")
    private Cita cita;

    // Ahora es opcional: tu PagoService (CU-04) todavía no lo asigna.
    // Si más adelante quieres registrarlo ahí también, agrega en CU-04:
    // pago.setPaciente(cita.getPaciente());
    @ManyToOne
    @JoinColumn(name = "paciente_id")
    private Usuario paciente;

    // Solo lo usa CU-06 (quién cobró en caja)
    @ManyToOne
    @JoinColumn(name = "cajero_id")
    private Usuario cajero;

    @Column(name = "numero_transaccion", nullable = false, unique = true, length = 100)
    private String numeroTransaccion;

    @Column(name = "monto_total", nullable = false)
    private BigDecimal monto;

    // Solo lo usa CU-06 (pago en efectivo)
    @Column(name = "monto_recibido")
    private BigDecimal montoRecibido;

    // Solo lo usa CU-06 (pago en efectivo)
    @Column(name = "cambio_devuelto")
    private BigDecimal cambioDevuelto = BigDecimal.ZERO;

    // CU-04 guarda "TARJETA" (genérico); CU-06 guarda "EFECTIVO"/"VISA"/"MASTERCARD"/"DEBITO"
    @Column(name = "metodo_pago", nullable = false, length = 20)
    private String metodoPago;

    @Column(name = "ultimos_cuatro_digitos", length = 4)
    private String ultimos4Tarjeta;

    // Solo lo usa CU-04
    @Column(name = "nombre_titular", length = 100)
    private String nombreTitular;

    // Solo lo usa CU-04 (ej. "APROBADO")
    @Column(name = "estado", length = 20)
    private String estado;

    @Column(name = "uuid_idempotencia", unique = true, length = 100)
    private String idempotencyKey;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();
}