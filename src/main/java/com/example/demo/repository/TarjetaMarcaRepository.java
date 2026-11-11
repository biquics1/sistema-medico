// ============================================================
// REPOSITORY: TarjetaMarcaRepository
// Catálogo de marcas de tarjeta y contador de usos (CU-04/CU-06/CU-14).
// ============================================================
package com.example.demo.repository;

import com.example.demo.modelo.TarjetaMarca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TarjetaMarcaRepository extends JpaRepository<TarjetaMarca, Integer>,
        JpaSpecificationExecutor<TarjetaMarca> {

    List<TarjetaMarca> findByEstadoOrderByNombreAsc(Short estado);

    Optional<TarjetaMarca> findByNombreIgnoreCaseAndEstado(String nombre, Short estado);

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Integer id);

    // Incremento atómico en BD: evita perder conteos si dos pagos se procesan a la vez.
    @Modifying
    @Query("UPDATE TarjetaMarca t SET t.totalUsos = t.totalUsos + 1 WHERE t.id = :id")
    int incrementarUsos(@Param("id") Integer id);
}
