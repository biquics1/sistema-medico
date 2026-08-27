// ============================================================
// ENTIDAD JPA: SignosVitales -> tabla "signos_vitales"
// Registro de los signos vitales tomados por enfermería antes
// de la consulta médica. Relación 1:1 con Cita. CU-07.
// Rangos de captura definidos en RN-CU07-01 a RN-CU07-05.
// ============================================================
package com.example.demo.modelo;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "signos_vitales")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignosVitales {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // PK autoincremental
    private Integer id;

    // Una cita solo puede tener un registro de signos vitales (UNIQUE en BD)
    @OneToOne
    @JoinColumn(name = "cita_id", nullable = false, unique = true) // FK -> cita asociada
    private Cita cita;

    @ManyToOne
    @JoinColumn(name = "enfermero_id", nullable = false) // FK -> usuario (enfermero que tomó los signos)
    private Usuario enfermero;

    // RN-CU07-01: 60-250 mmHg
    @Column(name = "presion_sistolica", nullable = false)
    private Integer presionSistolica;

    // RN-CU07-01: 40-150 mmHg
    @Column(name = "presion_diastolica", nullable = false)
    private Integer presionDiastolica;

    // RN-CU07-02: 34.0-42.0 °C
    @Column(nullable = false, precision = 4, scale = 1)
    private BigDecimal temperatura;

    // RN-CU07-03: 0.5-300 kg
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal peso;

    // RN-CU07-04: 30-250 cm
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal talla;

    // RN-CU07-05: 30-220 lpm
    @Column(name = "frecuencia_cardiaca", nullable = false)
    private Integer frecuenciaCardiaca;

    @Column(name = "es_emergencia", nullable = false)
    private boolean esEmergencia = false; // Marca si el paciente pasa directo a consulta médica (FA01)

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();
}
