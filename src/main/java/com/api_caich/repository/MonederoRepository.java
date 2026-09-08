package com.api_caich.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.api_caich.entity.Monedero;

@Repository
public interface MonederoRepository extends JpaRepository<Monedero, Long> {

    Optional<Monedero> findByAlumno_UsuarioId(Long alumnoId);

    boolean existsByAlumno_UsuarioId(Long alumnoId);
}
