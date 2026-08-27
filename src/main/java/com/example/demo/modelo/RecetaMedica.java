// ============================================================
// ENTIDAD JPA: RecetaMedica  ->  tabla "receta_medica"
// Cabecera de la receta generada al cierre de una Consulta
// Médica (CU-08, FA04). Contiene el detalle en
// DetalleRecetaMedica y es consumida por Farmacia (CU-11).
// ============================================================
package com.example.demo.modelo;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "receta_medica")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecetaMedica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // PK autoincremental
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "consulta_id", nullable = false) // FK -> consulta_medica que originó la receta
    private ConsultaMedica consulta;

    // NUEVO (CU-11): 1 = Activa (disponible para despacho), 0 = Inactiva
    @Column(nullable = false)
    private Short estado = 1;

    // NUEVO (CU-11): notas opcionales de la receta
    @Column(columnDefinition = "text")
    private String notas;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now(); // Fecha de emisión (usada para validar vigencia de 7 días)
}
