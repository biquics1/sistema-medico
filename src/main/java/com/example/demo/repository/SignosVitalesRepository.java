package com.example.demo.repository;

import com.example.demo.modelo.SignosVitales;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SignosVitalesRepository extends JpaRepository<SignosVitales, Integer> {

    Optional<SignosVitales> findByCita_Id(Integer citaId);

    boolean existsByCita_Id(Integer citaId);
}
