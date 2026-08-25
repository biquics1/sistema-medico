package com.example.demo.modelo;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cie10")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Cie10 {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 10)
    private String codigo;

    @Column(nullable = false, columnDefinition = "text")
    private String descripcion;
}
