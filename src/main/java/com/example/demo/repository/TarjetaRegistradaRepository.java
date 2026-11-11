package com.example.demo.repository;

import com.example.demo.modelo.TarjetaRegistrada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface TarjetaRegistradaRepository extends JpaRepository<TarjetaRegistrada, Integer>,
        JpaSpecificationExecutor<TarjetaRegistrada> {

    Optional<TarjetaRegistrada> findByHashNumero(String hashNumero);

    // Suma un uso de forma atómica en BD. Devuelve 0 si la tarjeta aún no existe.
    @Modifying
    @Query("UPDATE TarjetaRegistrada t SET t.vecesUsada = t.vecesUsada + 1, t.ultimoUso = :ahora "
            + "WHERE t.hashNumero = :hash")
    int sumarUso(@Param("hash") String hash, @Param("ahora") LocalDateTime ahora);
}