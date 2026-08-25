package com.example.demo.repository;

import com.example.demo.modelo.ReasignacionMedico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReasignacionMedicoRepository extends JpaRepository<ReasignacionMedico, Integer> {
}