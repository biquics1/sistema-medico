package com.example.demo.modelo;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "detalle_despacho_medicamento")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DetalleDespachoMedicamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "despacho_id", nullable = false)
    private DespachoMedicamento despacho;

    // Medicamento tal como fue recetado
    @ManyToOne
    @JoinColumn(name = "medicamento_id", nullable = false)
    private Medicamento medicamento;

    // Cantidad y precio de lo efectivamente entregado (del sustituto si sustituido = true)
    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    @Column(nullable = false)
    private boolean sustituido = false;

    // Medicamento efectivamente entregado en su lugar; solo se llena si sustituido = true
    @ManyToOne
    @JoinColumn(name = "medicamento_sustituto_id")
    private Medicamento medicamentoSustituto;

    @Column(name = "razon_sustitucion", columnDefinition = "text")
    private String razonSustitucion;
}
