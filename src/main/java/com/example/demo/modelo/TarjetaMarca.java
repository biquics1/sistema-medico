// ============================================================
// ENTIDAD JPA: TarjetaMarca -> tabla "tarjeta_marca"
// Catálogo de marcas de tarjeta aceptadas (Visa, Mastercard...).
// "patron" es la regex del número para detectar la marca (CU-04) y
// "totalUsos" cuenta cuántos pagos aprobados se hicieron con ella
// (CU-04 en línea y CU-06 caja).
// ============================================================
package com.example.demo.modelo;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tarjeta_marca")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TarjetaMarca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 30)
    private String nombre; // Siempre en mayúsculas: "VISA", "MASTERCARD"

    @Column(length = 200)
    private String descripcion;

    @Column(nullable = false, length = 200)
    private String patron; // Regex aplicada al número sin espacios

    @Column(name = "total_usos", nullable = false)
    private Long totalUsos = 0L; // Se incrementa de forma atómica (ver repository)

    @Column(nullable = false)
    private Short estado = 1; // 1 = Activa, 0 = Inactiva
}
