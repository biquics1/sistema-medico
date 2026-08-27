// ============================================================
// ENTIDAD JPA: Especialidad  ->  tabla "especialidad"
// Catálogo de especialidades médicas (Medicina General,
// Pediatría, etc.). Usado en CU-00, CU-03, CU-12.
// ============================================================
package com.example.demo.modelo;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "especialidad")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Especialidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // PK autoincremental
    private Integer id;

    @Column(nullable = false, length = 200)
    private String nombre; // Ej. "Pediatría"

    @Column(nullable = false, length = 500)
    private String descripcion; // Obligatoria en este catálogo (RN-CU15-01)

    @Column(nullable = false)
    private Short estado = 1; // 1 = Activo, 0 = Inactivo (valor por defecto: Activo)
}
