// ============================================================
// ENTIDAD JPA: CitaSeguimiento  ->  tabla "cita_seguimiento"
// Vincula una nueva Cita (citaNueva) como seguimiento de una
// Consulta Médica previa (consultaOrigen). CU-11/CU-12.
// Cada cita nueva solo puede ser seguimiento de UNA consulta
// (relación 1:1, UNIQUE en BD sobre cita_nueva_id).
// ============================================================
package com.example.demo.modelo;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "cita_seguimiento")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CitaSeguimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // PK autoincremental
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "consulta_origen_id", nullable = false) // FK -> consulta que generó el seguimiento
    private ConsultaMedica consultaOrigen;

    // Una cita nueva solo puede ser el seguimiento de UNA consulta (UNIQUE en BD)
    @OneToOne
    @JoinColumn(name = "cita_nueva_id", nullable = false, unique = true) // FK -> la cita de seguimiento creada
    private Cita citaNueva;

    @Column(name = "tipo_seguimiento", nullable = false, length = 30)
    private String tipoSeguimiento; // MONITOREO_TRATAMIENTO o REVISION_RESULTADOS (RN-CU11-01)

    @Column(name = "motivo_seguimiento", columnDefinition = "text")
    private String motivoSeguimiento; // Observaciones obligatorias (RN-CU11-03)

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    // NUEVO (requiere ALTER TABLE, ver seed/migración): controla que el
    // recordatorio (RN-CU11-05) se envíe una sola vez y sobreviva a
    // reinicios del sistema (RNF-020).
    @Column(name = "recordatorio_enviado", nullable = false)
    private boolean recordatorioEnviado = false;

    // Constantes con los valores válidos de tipoSeguimiento
    public static final String MONITOREO_TRATAMIENTO = "MONITOREO_TRATAMIENTO";
    public static final String REVISION_RESULTADOS = "REVISION_RESULTADOS";
}
