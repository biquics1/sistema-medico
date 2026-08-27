// ============================================================
// ENTIDAD JPA: InventarioMedicamento  ->  tabla "inventario_medicamento"
// Stock actual de un medicamento en una sucursal específica.
// Un mismo medicamento tiene un registro de stock por sucursal
// (UNIQUE medicamento_id + sucursal_id). Usado en CU-10/CU-13.
// ============================================================
package com.example.demo.modelo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "inventario_medicamento",
        uniqueConstraints = @UniqueConstraint(columnNames = {"medicamento_id", "sucursal_id"}))
@Getter
@Setter
@NoArgsConstructor
public class InventarioMedicamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // PK autoincremental
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicamento_id", nullable = false) // FK -> medicamento
    private Medicamento medicamento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_id", nullable = false) // FK -> sucursal
    private Sucursal sucursal;

    @Column(name = "stock_actual", nullable = false)
    private Integer stockActual = 0; // Cantidad disponible actualmente

    // Control de concurrencia optimista [RNF-025]
    @Version
    @Column(name = "version", nullable = false)
    private Integer version = 0; // JPA lo incrementa en cada UPDATE; evita choques entre movimientos simultáneos
}
