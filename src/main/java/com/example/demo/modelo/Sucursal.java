// ============================================================
// ENTIDAD JPA: Sucursal  ->  tabla "sucursal"
// Catálogo de sedes/sucursales del hospital. Usado en
// CU-00, CU-03, CU-12, CU-13.
// ============================================================
package com.example.demo.modelo;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sucursal")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Sucursal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // PK autoincremental
    private Integer id;

    @Column(nullable = false, length = 100)
    private String nombre; // Ej. "Sede Central"

    @Column(length = 8)
    private String telefono; // Opcional, 8 dígitos (RN-CU15-04)

    @Column(length = 500)
    private String direccion; // Opcional

    @Column(length = 250)
    private String descripcion; // Opcional

    @Column(nullable = false)
    private Short estado = 1; // 1 = Activo, 0 = Inactivo
}
