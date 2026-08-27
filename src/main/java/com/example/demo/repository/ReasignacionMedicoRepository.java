// ============================================================
// REPOSITORY: ReasignacionMedicoRepository
// Acceso a datos del historial de reasignaciones de médico
// (CU-05, FA07). No tiene métodos propios: usa solo los
// heredados de JpaRepository (save, findById, findAll, etc.).
// ============================================================
package com.example.demo.repository;

import com.example.demo.modelo.ReasignacionMedico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReasignacionMedicoRepository extends JpaRepository<ReasignacionMedico, Integer> {
}
