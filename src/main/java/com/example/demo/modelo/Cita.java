// ============================================================
// ENTIDAD JPA: Cita -> tabla "cita"
// Entidad central del sistema: representa una cita médica
// (agendada, walk-in o de seguimiento) y su ciclo de vida
// completo mediante estadoCita. CU-03, CU-05, CU-06, CU-08.
// ============================================================
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
    @GeneratedValue(strategy = GenerationType.IDENTITY) // PK autoincremental
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "paciente_id", nullable = false) // FK -> usuario (paciente)
    private Usuario paciente;

    @ManyToOne
    @JoinColumn(name = "medico_id", nullable = false) // FK -> usuario (médico asignado)
    private Usuario medico;

    @ManyToOne
    @JoinColumn(name = "sucursal_id", nullable = false) // FK -> sucursal donde se atiende
    private Sucursal sucursal;

    @ManyToOne
    @JoinColumn(name = "especialidad_id", nullable = false) // FK -> especialidad solicitada
    private Especialidad especialidad;

    @ManyToOne
    @JoinColumn(name = "estado_cita_id", nullable = false) // FK -> estado actual del ciclo de vida
    private EstadoCita estadoCita;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora; // Fecha/hora agendada (debe ser futura al crearse, RN-CU03-05)

    @Column(name = "motivo_consulta", nullable = false)
    private String motivoConsulta; // 10-2000 caracteres (RN-CU03-03)

    @Column(name = "es_emergencia", nullable = false)
    private boolean esEmergencia = false; // Prioridad de atención inmediata

    @Column(name = "es_walk_in", nullable = false)
    private boolean esWalkIn = false; // true si fue creada directamente en recepción (sin agendamiento previo)

    @Column(name = "hora_llegada")
    private LocalDateTime horaLlegada; // Se registra cuando recepción confirma la llegada (CU-05)

    @Column(nullable = false)
    private BigDecimal monto = new BigDecimal("150.00"); // Monto a cobrar por la consulta

    @Column(name = "expira_en")
    private LocalDateTime expiraEn; // Fin del día de la cita (fechaHora). Si sigue "Pendiente de pago" al pasar
    // la medianoche de ese día, el job la cancela automáticamente.

    @Column(name = "sesion_pago_expira_en")
    private LocalDateTime sesionPagoExpiraEn; // Ventana de 5 min para completar el pago EN LÍNEA (CU-04).
    // Es independiente de expiraEn: si vence, NO cancela la cita,
    // solo invalida esa sesión de pago con tarjeta.

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    @Column(name = "veces_llamado_enfermeria", nullable = false)
    private Short vecesLlamadoEnfermeria = 0; // CU-07: cuantas veces se ha llamado al paciente (maximo 3: 1 inicial + 2 "Llamar de nuevo")

    @Column(name = "veces_llamado_medico", nullable = false)
    private Short vecesLlamadoMedico = 0; // CU-08: cuantas veces se ha llamado al paciente a consulta (maximo 3: 1 inicial + 2 "Llamar de nuevo")

    // Bloqueo optimista (RNF-025): evita que caja y recepción
    // pisen el mismo cambio de estado sin darse cuenta.
    @Version
    private Integer version; // Incrementado automáticamente por JPA en cada UPDATE
}