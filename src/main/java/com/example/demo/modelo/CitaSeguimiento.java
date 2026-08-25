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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "consulta_origen_id", nullable = false)
    private ConsultaMedica consultaOrigen;

    // Una cita nueva solo puede ser el seguimiento de UNA consulta (UNIQUE en BD)
    @OneToOne
    @JoinColumn(name = "cita_nueva_id", nullable = false, unique = true)
    private Cita citaNueva;

    @Column(name = "tipo_seguimiento", nullable = false, length = 30)
    private String tipoSeguimiento;

    @Column(name = "motivo_seguimiento", columnDefinition = "text")
    private String motivoSeguimiento;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    // NUEVO (requiere ALTER TABLE, ver seed/migración): controla que el
    // recordatorio (RN-CU11-05) se envíe una sola vez y sobreviva a
    // reinicios del sistema (RNF-020).
    @Column(name = "recordatorio_enviado", nullable = false)
    private boolean recordatorioEnviado = false;

    public static final String MONITOREO_TRATAMIENTO = "MONITOREO_TRATAMIENTO";
    public static final String REVISION_RESULTADOS = "REVISION_RESULTADOS";
}
