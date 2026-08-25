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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "consulta_id", nullable = false)
    private ConsultaMedica consulta;

    // NUEVO (CU-11): 1 = Activa (disponible para despacho), 0 = Inactiva
    @Column(nullable = false)
    private Short estado = 1;

    // NUEVO (CU-11): notas opcionales de la receta
    @Column(columnDefinition = "text")
    private String notas;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();
}
