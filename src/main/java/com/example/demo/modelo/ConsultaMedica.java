// ============================================================
// ENTIDAD JPA: ConsultaMedica -> tabla "consulta_medica"
// Registro de la consulta médica realizada durante una Cita:
// motivo, hallazgos, diagnóstico (CIE-10) y plan de tratamiento.
// Relación 1:1 con Cita (una cita = una consulta). CU-08.
// ============================================================
package com.example.demo.modelo;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "consulta_medica")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConsultaMedica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // PK autoincremental
    private Integer id;

    // Una cita solo puede tener una consulta médica (UNIQUE en BD)
    @OneToOne
    @JoinColumn(name = "cita_id", nullable = false, unique = true) // FK -> cita asociada
    private Cita cita;

    @ManyToOne
    @JoinColumn(name = "medico_id", nullable = false) // FK -> usuario (médico que atiende)
    private Usuario medico;

    // Obligatorio (RN-CU08-02)
    @Column(name = "motivo_visita", nullable = false, columnDefinition = "text")
    private String motivoVisita;

    @Column(name = "hallazgos_clinicos", columnDefinition = "text")
    private String hallazgosClinicos; // Anamnesis / exploración física

    @ManyToOne
    @JoinColumn(name = "cie10_id") // FK -> código CIE-10 (opcional)
    private Cie10 cie10;

    // Obligatorio solo para cerrar/finalizar (RN-CU08-01)
    @Column(columnDefinition = "text")
    private String diagnostico;

    @Column(name = "plan_tratamiento", columnDefinition = "text")
    private String planTratamiento;

    @Column(name = "notas_adicionales", columnDefinition = "text")
    private String notasAdicionales;

    @Column(nullable = false)
    private boolean finalizada = false; // true cuando el médico cierra la consulta

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();
}
