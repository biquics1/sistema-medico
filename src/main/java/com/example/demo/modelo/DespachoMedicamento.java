package com.example.demo.modelo;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "despacho_medicamento")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DespachoMedicamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Ahora es opcional: null cuando es venta libre (sin receta)
    @ManyToOne
    @JoinColumn(name = "receta_id")
    private RecetaMedica receta;

    @ManyToOne
    @JoinColumn(name = "farmaceutico_id", nullable = false)
    private Usuario farmaceutico;

    @ManyToOne
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;

    // NUEVO: true si el despacho no viene de una receta (venta directa de mostrador)
    @Column(name = "es_venta_libre", nullable = false)
    private boolean esVentaLibre = false;

    // NUEVO: paciente indicado manualmente en venta libre (opcional).
    // Cuando hay receta, el paciente se obtiene vía receta -> consulta -> cita -> paciente.
    @ManyToOne
    @JoinColumn(name = "paciente_id")
    private Usuario paciente;

    // NUEVO: pago asociado al carrito (un mismo pago puede cubrir varios despachos del carrito)
    @ManyToOne
    @JoinColumn(name = "pago_id")
    private Pago pago;

    @Column(name = "monto_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoTotal = BigDecimal.ZERO;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();
}
