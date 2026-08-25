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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicamento_id", nullable = false)
    private Medicamento medicamento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;

    @Column(name = "stock_actual", nullable = false)
    private Integer stockActual = 0;

    // Control de concurrencia optimista [RNF-025]
    @Version
    @Column(name = "version", nullable = false)
    private Integer version = 0;
}
