package com.example.demo.repository;

import com.example.demo.modelo.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Integer> {

    // Usado por PagoService (CU-04)
    Optional<Pago> findByIdempotencyKey(String idempotencyKey);

    // Usado por CajaService (CU-06)
    boolean existsByIdempotencyKey(String idempotencyKey);
}