// ============================================================
// ENTIDAD JPA: BranchSpecialty  ->  tabla "sucursal_especialidad"
// Relación N:M entre Sucursal y Especialidad (CU-12/CU-13).
// Solo se permite Crear y Eliminar (sin edición), según reglas
// de negocio RN-CU12-01.
// ============================================================
package com.example.demo.modelo;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "sucursal_especialidad", uniqueConstraints = {
        // Evita asignar la misma combinación sede+especialidad dos veces
        @UniqueConstraint(columnNames = {"sucursal_id", "especialidad_id"})
})
@Data // Lombok: genera getters, setters, equals, hashCode y toString
public class BranchSpecialty {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) // PK autoincremental
    private Integer id;

    @ManyToOne @JoinColumn(name = "sucursal_id", nullable = false) // FK -> sucursal
    private Sucursal sucursal;

    @ManyToOne @JoinColumn(name = "especialidad_id", nullable = false) // FK -> especialidad
    private Especialidad especialidad;

    private Short estado; // 1 = Activo, 0 = Inactivo
}
