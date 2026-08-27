// ============================================================
// ENTIDAD JPA: Rol  ->  tabla "rol"
// Catálogo de roles del sistema (Médico, Enfermero,
// Recepcionista, Cajero, Laboratorista, Farmacéutico,
// Administrador, Paciente). Usado en CU-01.
// ============================================================

package com.example.demo.modelo;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "rol")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // PK autoincremental
    private Integer id;

    @Column(nullable = false, unique = true, length = 200)
    private String nombre; // Ej. "Médico" (único en el catálogo)

    @Column(length = 500)
    private String descripcion; // Opcional

    @Column(nullable = false)
    private Short estado = 1; // 1 = Activo, 0 = Inactivo
}
