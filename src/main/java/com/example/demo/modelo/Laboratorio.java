// ============================================================
// ENTIDAD JPA: Laboratorio  ->  tabla "laboratorio"
// Catálogo de laboratorios clínicos que agrupan exámenes
// (ExamenLaboratorio). Usado en CU-09.
// ============================================================
package com.example.demo.modelo;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "laboratorio")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Laboratorio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // PK autoincremental
    private Integer id;

    @Column(nullable = false, length = 200)
    private String nombre; // Ej. "Laboratorio Clínico Central"

    @Column(length = 500)
    private String descripcion; // Opcional

    @Column(nullable = false)
    private Short estado = 1; // 1 = Activo, 0 = Inactivo
}
