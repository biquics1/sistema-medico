package com.example.demo.repository;

import com.example.demo.modelo.BranchSpecialty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BranchSpecialtyRepository extends JpaRepository<BranchSpecialty, Integer> {

    List<BranchSpecialty> findBySucursal_IdAndEstado(Integer sucursalId, Short estado);

    boolean existsBySucursal_IdAndEspecialidad_Id(Integer sucursalId, Integer especialidadId);
}