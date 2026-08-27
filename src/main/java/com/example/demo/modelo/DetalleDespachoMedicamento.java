// ============================================================
// ENTIDAD JPA: DetalleDespachoMedicamento -> tabla "detalle_despacho_medicamento"
// Cada línea de un despacho de farmacia: qué medicamento se
// entregó realmente (puede ser un sustituto), cantidad y
// precio unitario. CU-11 (Despacho de Medicamentos), FA02.
// ============================================================
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
    @GeneratedValue(strategy = GenerationType.IDENTITY) // PK autoincremental
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "despacho_id", nullable = false) // FK -> despacho_medicamento (cabecera)
    private DespachoMedicamento despacho;

    // Medicamento tal como fue recetado
    @ManyToOne
    @JoinColumn(name = "medicamento_id", nullable = false) // FK -> medicamento original de la receta
    private Medicamento medicamento;

    // Cantidad y precio de lo efectivamente entregado (del sustituto si sustituido = true)
    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    @Column(nullable = false)
    private boolean sustituido = false; // true si se entregó un medicamento distinto al recetado

    // Medicamento efectivamente entregado en su lugar; solo se llena si sustituido = true
    @ManyToOne
    @JoinColumn(name = "medicamento_sustituto_id")
    private Medicamento medicamentoSustituto;

    @Column(name = "razon_sustitucion", columnDefinition = "text")
    private String razonSustitucion; // Obligatorio solo cuando sustituido = true (FA02)
}
