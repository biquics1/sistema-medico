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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "cita_id", nullable = false)
    private Cita cita;

    @ManyToOne
    @JoinColumn(name = "medico_anterior_id", nullable = false)
    private Usuario medicoAnterior;

    @ManyToOne
    @JoinColumn(name = "medico_nuevo_id", nullable = false)
    private Usuario medicoNuevo;

    @ManyToOne
    @JoinColumn(name = "usuario_reasigna_id", nullable = false)
    private Usuario usuarioReasigna;

    private String motivo;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();
}