// ============================================================
// ENTIDAD JPA: Medicamento  ->  tabla "medicamento"
// Catálogo maestro de medicamentos. Usado por recetas
// (DetalleRecetaMedica), inventario (InventarioMedicamento) y
// despachos (DetalleDespachoMedicamento). CU-10/CU-13/CU-15.
// ============================================================
package com.example.demo.modelo;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "medicamento")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Medicamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // PK autoincremental
    private Integer id;

    @Column(nullable = false, length = 200)
    private String nombre; // Ej. "Paracetamol 500mg"

    @Column(nullable = false, length = 500)
    private String descripcion; // Obligatoria (RN-CU15-01)

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio; // Precio de venta, debe ser > 0

    @Column(nullable = false, length = 50)
    private String unidad; // Ej. "tableta", "ml" (RN-CU15-02)

    @Column(name = "es_controlado", nullable = false)
    private boolean esControlado = false; // Si es true, requiere auditoría especial en despacho [RNF-017]

    @Column(name = "stock_minimo")
    private Integer stockMinimo; // Opcional, usado para alertas de stock bajo (RN-CU10-03)

    @Column(nullable = false)
    private Short estado = 1; // 1 = Activo, 0 = Inactivo
}
