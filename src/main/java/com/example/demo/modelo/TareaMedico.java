// ============================================================
// ENTIDAD JPA: TareaMedico  ->  tabla "tarea_medico"
// Tareas/recordatorios personales del médico, gestionados
// desde el panel lateral TaskPanel en la Agenda Médica
// (CU-14, FA06). RN-CU14-02.
// ============================================================
package com.example.demo.modelo;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tarea_medico")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TareaMedico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // PK autoincremental
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "medico_id", nullable = false) // FK -> usuario (médico dueño de la tarea)
    private Usuario medico;

    @Column(nullable = false, length = 200)
    private String titulo; // Título de la tarea

    @Column(length = 1000)
    private String descripcion; // Opcional, máx 1000 caracteres

    // 0=Baja, 1=Normal, 2=Alta (RN-CU14-02)
    @Column(nullable = false)
    private Short prioridad = 1;

    // Opcional según esquema de BD (columna nullable) y Reglas Consolidadas.
    @Column(name = "fecha_limite")
    private LocalDate fechaLimite;

    @Column(nullable = false)
    private Boolean completada = false; // Pendiente (false) o Completada (true)

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();
}
