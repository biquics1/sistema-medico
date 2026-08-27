// ============================================================
// ENTIDAD JPA: EventoAgenda  ->  tabla "evento_agenda"
// Eventos personales del médico en su calendario (reuniones,
// descansos, capacitaciones, etc.), mostrados en violeta junto
// a las citas médicas. CU-14, RN-CU14-01.
// ============================================================
package com.example.demo.modelo;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "evento_agenda")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventoAgenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // PK autoincremental
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "medico_id", nullable = false) // FK -> usuario (médico dueño del evento)
    private Usuario medico;

    @Column(nullable = false, length = 200)
    private String titulo; // Ej. "Capacitación en congreso médico"

    @Column(length = 2000)
    private String descripcion; // Opcional, máx 500/2000 caracteres según validación

    // 0=Reunión, 1=Descanso, 2=Capacitación, 3=Personal, 4=Otro (RN-CU14-01)
    @Column(name = "tipo_evento", nullable = false)
    private Short tipoEvento;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio; // Obligatoria

    @Column(name = "fecha_fin", nullable = false)
    private LocalDateTime fechaFin; // Obligatoria, debe ser posterior a fechaInicio

    @Column(name = "todo_el_dia", nullable = false)
    private Boolean todoElDia = false; // Si es true, el frontend usa solo fecha (sin hora)

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();
}
