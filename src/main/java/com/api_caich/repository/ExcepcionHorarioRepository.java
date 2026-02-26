package com.api_caich.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.api_caich.entity.ExcepcionHorario;

@Repository
public interface ExcepcionHorarioRepository extends JpaRepository<ExcepcionHorario, Long> {

    /**
     * Obtiene las excepciones globales para una fecha (sin espacio asociado).
     */
    @Query("SELECT e FROM ExcepcionHorario e WHERE e.espacio IS NULL AND e.fecha = :fecha AND e.estaActivo = true")
    List<ExcepcionHorario> findExcepcionesGlobalesPorFecha(@Param("fecha") LocalDate fecha);

    /**
     * Obtiene las excepciones globales en un rango de fechas.
     */
    @Query("SELECT e FROM ExcepcionHorario e WHERE e.espacio IS NULL AND e.fecha BETWEEN :fechaInicio AND :fechaFin AND e.estaActivo = true")
    List<ExcepcionHorario> findExcepcionesGlobalesPorFechaBetween(@Param("fechaInicio") LocalDate fechaInicio, @Param("fechaFin") LocalDate fechaFin);

    /**
     * Obtiene las excepciones para una fecha específica de un espacio.
     */
    List<ExcepcionHorario> findByEspacioEspacioIdAndFechaAndEstaActivoTrue(Long espacioId, LocalDate fecha);

    /**
     * Obtiene las excepciones para una fecha en cualquier espacio.
     */
    @Query("SELECT e FROM ExcepcionHorario e WHERE e.fecha = :fecha AND e.estaActivo = true")
    List<ExcepcionHorario> findByFechaAndEstaActivoTrue(@Param("fecha") LocalDate fecha);

    /**
     * Obtiene excepciones de un espacio en un rango de fechas.
     */
    @Query("SELECT e FROM ExcepcionHorario e WHERE e.espacio.espacioId = :espacioId AND e.fecha BETWEEN :fechaInicio AND :fechaFin AND e.estaActivo = true")
    List<ExcepcionHorario> findByEspacioAndFechaBetween(@Param("espacioId") Long espacioId, @Param("fechaInicio") LocalDate fechaInicio, @Param("fechaFin") LocalDate fechaFin);

    /**
     * Verifica si existe excepción para una fecha específica de un espacio.
     */
    boolean existsByEspacioEspacioIdAndFechaAndEstaActivoTrue(Long espacioId, LocalDate fecha);

    /**
     * Obtiene todas las excepciones activas.
     */
    List<ExcepcionHorario> findByEstaActivoTrue();
}
