package com.api_caich.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.api_caich.entity.HorarioBase;

@Repository
public interface HorarioBaseRepository extends JpaRepository<HorarioBase, Long> {

    /**
     * Obtiene el horario base para un espacio específico.
     */
    Optional<HorarioBase> findByEspacioEspacioId(Long espacioId);

    /**
     * Obtiene el horario base global (sin espacio asociado).
     */
    @Query("SELECT h FROM HorarioBase h WHERE h.espacio IS NULL")
    Optional<HorarioBase> findHorarioBaseGlobal();

    /**
     * Obtiene todos los horarios base activos.
     */
    List<HorarioBase> findByEstaActivoTrue();

    /**
     * Verifica si existe un horario base para un espacio.
     */
    boolean existsByEspacioEspacioId(Long espacioId);
}
