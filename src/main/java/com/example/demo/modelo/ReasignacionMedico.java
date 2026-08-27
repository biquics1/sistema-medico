// ============================================================
// ENTIDAD JPA: ReasignacionMedico  ->  tabla "reasignacion_medico"
// Registro histórico/auditable de cada vez que se cambia el
// médico asignado a una Cita (CU-05, FA07 - Reasignación de
// médico).
// ============================================================
package com.example.demo.modelo;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "reasignacion_medico")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReasignacionMedico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // PK autoincremental
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "cita_id", nullable = false) // FK -> cita reasignada
    private Cita cita;

    @ManyToOne
    @JoinColumn(name = "medico_anterior_id", nullable = false) // FK -> usuario (médico saliente)
    private Usuario medicoAnterior;

    @ManyToOne
    @JoinColumn(name = "medico_nuevo_id", nullable = false) // FK -> usuario (médico entrante)
    private Usuario medicoNuevo;

    @ManyToOne
    @JoinColumn(name = "usuario_reasigna_id", nullable = false) // FK -> usuario que ejecutó la reasignación (recepcionista)
    private Usuario usuarioReasigna;

    private String motivo; // Nota opcional con el motivo de la reasignación

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now(); // Fecha/hora del cambio
}
