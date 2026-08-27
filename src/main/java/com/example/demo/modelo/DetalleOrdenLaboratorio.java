// ============================================================
// ENTIDAD JPA: DetalleOrdenLaboratorio -> tabla "detalle_orden_laboratorio"
// Cada examen incluido dentro de una Orden de Laboratorio,
// junto con su resultado una vez procesado. CU-08 (generación)
// y CU-09 (registro/publicación de resultados).
// ============================================================
package com.example.demo.modelo;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "detalle_orden_laboratorio")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DetalleOrdenLaboratorio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // PK autoincremental
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "orden_id", nullable = false) // FK -> orden_laboratorio (cabecera)
    private OrdenLaboratorio orden;

    @ManyToOne
    @JoinColumn(name = "examen_id", nullable = false) // FK -> examen_laboratorio solicitado
    private ExamenLaboratorio examen;

    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario; // Precio del examen al momento de ordenarlo

    @Column(name = "valor_resultado", length = 100)
    private String valorResultado; // Resultado capturado por el laboratorista

    @Column(length = 30)
    private String unidad; // Unidad de medida del resultado

    @Column(name = "fuera_de_rango", nullable = false)
    private boolean fueraDeRango = false; // Marcado manual si el valor está fuera del rango de referencia (FA02)

    @Column(name = "notas_resultado", columnDefinition = "text")
    private String notasResultado; // Notas adicionales del resultado

    @Column(nullable = false)
    private boolean publicado = false; // true una vez publicado (visible para el médico)

    @Column(name = "fecha_resultado")
    private LocalDateTime fechaResultado; // Fecha en que se registró/guardó el resultado
}
