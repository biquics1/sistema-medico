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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Una cita solo puede tener una consulta médica (UNIQUE en BD)
    @OneToOne
    @JoinColumn(name = "cita_id", nullable = false, unique = true)
    private Cita cita;

    @ManyToOne
    @JoinColumn(name = "medico_id", nullable = false)
    private Usuario medico;

    // Obligatorio (RN-CU08-02)
    @Column(name = "motivo_visita", nullable = false, columnDefinition = "text")
    private String motivoVisita;

    @Column(name = "hallazgos_clinicos", columnDefinition = "text")
    private String hallazgosClinicos;

    @ManyToOne
    @JoinColumn(name = "cie10_id")
    private Cie10 cie10;

    // Obligatorio solo para cerrar/finalizar (RN-CU08-01)
    @Column(columnDefinition = "text")
    private String diagnostico;

    @Column(name = "plan_tratamiento", columnDefinition = "text")
    private String planTratamiento;

    @Column(name = "notas_adicionales", columnDefinition = "text")
    private String notasAdicionales;

    @Column(nullable = false)
    private boolean finalizada = false;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();
}
