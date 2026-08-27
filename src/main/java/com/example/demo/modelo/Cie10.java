// ============================================================
// ENTIDAD JPA: Cie10  ->  tabla "cie10"
// Catálogo de códigos de diagnóstico CIE-10 usado en la
// Consulta Médica (CU-08) con autocompletado [RNF-004].
// ============================================================
package com.example.demo.modelo;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cie10")
@Data                 // Getters/Setters/equals/hashCode/toString automáticos
@NoArgsConstructor     // Constructor vacío (requerido por JPA)
@AllArgsConstructor    // Constructor con todos los campos
public class Cie10 {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // PK autoincremental
    private Integer id;

    @Column(nullable = false, unique = true, length = 10)
    private String codigo; // Ej. "J00" (código CIE-10)

    @Column(nullable = false, columnDefinition = "text")
    private String descripcion; // Ej. "Rinofaringitis aguda (resfriado común)"
}
