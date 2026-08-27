// ============================================================
// ENTIDAD JPA: OrdenLaboratorio -> tabla "orden_laboratorio"
// Cabecera de una orden de exámenes de laboratorio generada
// por el médico durante la consulta (CU-08, FA01) y procesada
// por el personal de laboratorio (CU-09) tras el cobro (CU-16).
// ============================================================
package com.example.demo.modelo;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orden_laboratorio")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrdenLaboratorio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // PK autoincremental
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "consulta_id") // FK -> consulta_medica que originó la orden (opcional)
    private ConsultaMedica consulta;

    @ManyToOne
    @JoinColumn(name = "paciente_id", nullable = false) // FK -> usuario (paciente)
    private Usuario paciente;

    @ManyToOne
    @JoinColumn(name = "medico_id", nullable = false) // FK -> usuario (médico que ordenó)
    private Usuario medico;

    @Column(name = "es_externa", nullable = false)
    private boolean esExterna = false; // true si el paciente hará los exámenes en un laboratorio externo

    // 0=Pendiente, 1=En proceso, 2=Completada, 3=Cancelada (CHECK en BD)
    @Column(nullable = false)
    private Short estado = 0;

    @Column(name = "monto_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoTotal = BigDecimal.ZERO; // Suma de los exámenes solicitados

    @ManyToOne
    @JoinColumn(name = "pago_id") // FK -> pago (se asigna cuando se cobra en CU-16)
    private Pago pago;

    @Column(columnDefinition = "text")
    private String notas; // Observaciones del médico

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    // Constantes de estado (coinciden con RN de CU-09/CU-16)
    public static final short PENDIENTE = 0;
    public static final short EN_PROCESO = 1;
    public static final short COMPLETADA = 2;
    public static final short CANCELADA = 3;
}
