// ============================================================
// REPOSITORY: PagoRepository
// Acceso a datos de los pagos (CU-04/CU-06/CU-16).
// ============================================================
package com.example.demo.repository;

import com.example.demo.modelo.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Integer> {

    // Usado por PagoService (CU-04)
    // Busca un pago por su clave de idempotencia, para evitar procesarlo dos veces
    Optional<Pago> findByIdempotencyKey(String idempotencyKey);

    // Usado por CajaService (CU-06)
    boolean existsByIdempotencyKey(String idempotencyKey);
}
