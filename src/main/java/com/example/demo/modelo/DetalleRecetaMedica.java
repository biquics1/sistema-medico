// ============================================================
// ENTIDAD JPA: DetalleRecetaMedica  ->  tabla "detalle_receta_medica"
// Cada línea de una receta médica: un medicamento con su
// dosis, frecuencia y duración (RN-CU08-03).
// ============================================================
package com.example.demo.modelo;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "detalle_receta_medica")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DetalleRecetaMedica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // PK autoincremental
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "receta_id", nullable = false) // FK -> receta_medica (cabecera)
    private RecetaMedica receta;

    @ManyToOne
    @JoinColumn(name = "medicamento_id", nullable = false) // FK -> medicamento recetado
    private Medicamento medicamento;

    @Column(nullable = false, length = 100)
    private String dosis; // Ej. "500mg"

    @Column(nullable = false, length = 100)
    private String frecuencia; // Ej. "Cada 8 horas"

    @Column(nullable = false, length = 100)
    private String duracion; // Ej. "7 días"

    @Column(columnDefinition = "text")
    private String indicaciones; // Opcional, indicaciones especiales
}
