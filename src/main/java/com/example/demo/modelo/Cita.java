package com.example.demo.modelo;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cita")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Cita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "paciente_id", nullable = false)
    private Usuario paciente;

    @ManyToOne
    @JoinColumn(name = "medico_id", nullable = false)
    private Usuario medico;

    @ManyToOne
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;

    @ManyToOne
    @JoinColumn(name = "especialidad_id", nullable = false)
    private Especialidad especialidad;

    @ManyToOne
    @JoinColumn(name = "estado_cita_id", nullable = false)
    private EstadoCita estadoCita;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "motivo_consulta", nullable = false)
    private String motivoConsulta;

    @Column(name = "es_emergencia", nullable = false)
    private boolean esEmergencia = false;

    @Column(name = "es_walk_in", nullable = false)
    private boolean esWalkIn = false;

    @Column(name = "hora_llegada")
    private LocalDateTime horaLlegada;

    @Column(nullable = false)
    private BigDecimal monto = new BigDecimal("150.00");

    @Column(name = "expira_en")
    private LocalDateTime expiraEn;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    // Bloqueo optimista (RNF-025): evita que caja y recepción
    // pisen el mismo cambio de estado sin darse cuenta.
    @Version
    private Integer version;
}