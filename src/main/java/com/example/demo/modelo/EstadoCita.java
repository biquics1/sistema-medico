package com.example.demo.modelo;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "estado_cita")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EstadoCita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String nombre;

    @Column(length = 200)
    private String descripcion;

    @Column(nullable = false)
    private Short estado = 1;

    // Nombres tal como quedaron sembrados por 02_crear_schema_completo.sql
    public static final String PENDIENTE_PAGO = "Pendiente de pago";
    public static final String CONFIRMADA = "Confirmada";
    public static final String PACIENTE_PRESENTE = "Paciente Presente";
    public static final String SIGNOS_VITALES = "Signos Vitales";
    public static final String EN_ESPERA = "En Espera";
    public static final String CONSULTA_MEDICA = "Consulta Médica";
    public static final String EVALUADO = "Evaluado";
    public static final String ATENCION_FINALIZADA = "Atención Finalizada";
    public static final String NO_ASISTIO = "No Asistió";
    public static final String CANCELADA = "Cancelada";
}