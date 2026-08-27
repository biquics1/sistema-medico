// ============================================================
// REPOSITORY: Cie10Repository
// Acceso a datos del catálogo CIE-10. Provee el autocompletado
// usado en el formulario de Consulta Médica (CU-08, paso 6).
// ============================================================
package com.example.demo.repository;

import com.example.demo.modelo.Cie10;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface Cie10Repository extends JpaRepository<Cie10, Integer> {

    // CU-08, paso 6: autocompletado por código o descripción
    // JPQL personalizado: busca coincidencias parciales (LIKE) sin distinguir mayúsculas/minúsculas
    @Query("""
            SELECT c FROM Cie10 c
            WHERE UPPER(c.codigo) LIKE UPPER(CONCAT('%', :texto, '%'))
               OR UPPER(c.descripcion) LIKE UPPER(CONCAT('%', :texto, '%'))
            ORDER BY c.codigo
            """)
    List<Cie10> buscar(@Param("texto") String texto);
}
