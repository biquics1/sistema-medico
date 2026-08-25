package com.example.demo.dto;

import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class DoctorDisponibleDTO {
    private Integer id;
    private String nombreCompleto;
}