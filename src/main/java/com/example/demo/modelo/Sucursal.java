package com.example.demo.modelo;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sucursal")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Sucursal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 8)
    private String telefono;

    @Column(length = 500)
    private String direccion;

    @Column(length = 250)
    private String descripcion;

    @Column(nullable = false)
    private Short estado = 1;
}