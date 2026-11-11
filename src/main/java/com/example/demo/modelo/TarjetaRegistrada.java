// ============================================================
// ENTIDAD JPA: TarjetaRegistrada -> tabla "tarjeta_registrada"
// Historial de tarjetas distintas usadas en pagos en línea (CU-04).
// Se identifica por un HMAC del número (hashNumero); el número completo
// y el CVV NUNCA se guardan [RNF-012].
// ============================================================
package com.example.demo.modelo;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tarjeta_registrada")
@Getter
@Setter
@NoArgsConstructor
public class TarjetaRegistrada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "hash_numero", nullable = false, unique = true, length = 64, updatable = false)
    private String hashNumero;

    @ManyToOne
    @JoinColumn(name = "marca_id")
    private TarjetaMarca marca;

    @Column(name = "ultimos_cuatro", nullable = false, length = 4)
    private String ultimosCuatro;

    @Column(name = "nombre_titular", length = 100)
    private String nombreTitular;

    @Column(name = "veces_usada", nullable = false)
    private Integer vecesUsada = 1;

    @Column(name = "primer_uso", nullable = false)
    private LocalDateTime primerUso;

    @Column(name = "ultimo_uso", nullable = false)
    private LocalDateTime ultimoUso;
}
