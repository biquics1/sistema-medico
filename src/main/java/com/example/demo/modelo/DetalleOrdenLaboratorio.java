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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "orden_id", nullable = false)
    private OrdenLaboratorio orden;

    @ManyToOne
    @JoinColumn(name = "examen_id", nullable = false)
    private ExamenLaboratorio examen;

    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    @Column(name = "valor_resultado", length = 100)
    private String valorResultado;

    @Column(length = 30)
    private String unidad;

    @Column(name = "fuera_de_rango", nullable = false)
    private boolean fueraDeRango = false;

    @Column(name = "notas_resultado", columnDefinition = "text")
    private String notasResultado;

    @Column(nullable = false)
    private boolean publicado = false;

    @Column(name = "fecha_resultado")
    private LocalDateTime fechaResultado;
}
