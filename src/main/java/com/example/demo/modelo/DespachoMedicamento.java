// ============================================================
// ENTIDAD JPA: DespachoMedicamento -> tabla "despacho_medicamento"
// Cabecera de un despacho de farmacia: puede originarse desde
// una receta médica o ser una venta libre (sin receta).
// CU-11 (Despacho de Medicamentos).
// ============================================================
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
    @GeneratedValue(strategy = GenerationType.IDENTITY) // PK autoincremental
    private Integer id;

    // Ahora es opcional: null cuando es venta libre (sin receta)
    @ManyToOne
    @JoinColumn(name = "receta_id") // FK -> receta_medica (puede ser null)
    private RecetaMedica receta;

    @ManyToOne
    @JoinColumn(name = "farmaceutico_id", nullable = false) // FK -> usuario (quién despachó)
    private Usuario farmaceutico;

    @ManyToOne
    @JoinColumn(name = "sucursal_id", nullable = false) // FK -> sucursal donde se despachó
    private Sucursal sucursal;

    // NUEVO: true si el despacho no viene de una receta (venta directa de mostrador)
    @Column(name = "es_venta_libre", nullable = false)
    private boolean esVentaLibre = false;

    // NUEVO: paciente indicado manualmente en venta libre (opcional).
    // Cuando hay receta, el paciente se obtiene vía receta -> consulta -> cita -> paciente.
    @ManyToOne
    @JoinColumn(name = "paciente_id") // FK -> usuario (paciente), opcional
    private Usuario paciente;

    // NUEVO: pago asociado al carrito (un mismo pago puede cubrir varios despachos del carrito)
    @ManyToOne
    @JoinColumn(name = "pago_id") // FK -> pago
    private Pago pago;

    @Column(name = "monto_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoTotal = BigDecimal.ZERO; // Suma de todos los detalles del despacho

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();
}
