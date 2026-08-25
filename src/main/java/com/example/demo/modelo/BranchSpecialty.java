package com.example.demo.modelo;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "sucursal_especialidad", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"sucursal_id", "especialidad_id"})
})
@Data
public class BranchSpecialty {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;

    @ManyToOne @JoinColumn(name = "especialidad_id", nullable = false)
    private Especialidad especialidad;

    private Short estado;
}