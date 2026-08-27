// ============================================================
// ENTIDAD JPA: ExamenLaboratorio  ->  tabla "examen_laboratorio"
// Catálogo de exámenes de laboratorio (Hemograma, Glucosa,
// etc.), cada uno perteneciente a un Laboratorio. Usado en
// CU-08 (orden de laboratorio) y CU-09 (procesamiento).
// ============================================================
package com.example.demo.modelo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "examen_laboratorio")
@Getter
@Setter
@NoArgsConstructor
public class ExamenLaboratorio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // PK autoincremental
    private Integer id;

    @Column(length = 20, unique = true)
    private String codigo; // Ej. "HEMO001", opcional pero único

    @Column(nullable = false, length = 200)
    private String nombre; // Ej. "Hemograma completo"

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio; // Debe ser > 0 (RN-CU15-03)

    @Column(name = "rango_referencia", length = 100)
    private String rangoReferencia; // Ej. "4.5-5.5", usado para alertas fuera de rango

    @Column(name = "unidad_medida", length = 30)
    private String unidadMedida; // Ej. "millones/uL"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "laboratorio_id", nullable = false) // FK -> laboratorio al que pertenece
    private Laboratorio laboratorio;

    @Column(nullable = false)
    private Short estado = 1; // 1 = Activo, 0 = Inactivo
}
