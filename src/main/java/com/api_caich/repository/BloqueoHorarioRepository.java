package com.api_caich.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.api_caich.entity.BloqueoHorario;

@Repository
public interface BloqueoHorarioRepository extends JpaRepository<BloqueoHorario, Long> {

    // CORREGIDO: Usar CAST para comparar LocalDate con LocalDateTime
    @Query("SELECT b FROM BloqueoHorario b WHERE " +
           "((b.espacio IS NULL OR b.espacio.espacioId = :espacioId)) AND " +
           "(:fecha BETWEEN CAST(b.fechaInicio AS date) AND CAST(b.fechaFin AS date))")
    List<BloqueoHorario> findBloqueosActivos(
        @Param("espacioId") Long espacioId, 
        @Param("fecha") LocalDate fecha
    );

    // Método alternativo: Convertir LocalDate a LocalDateTime en el servicio
    @Query("SELECT b FROM BloqueoHorario b WHERE " +
           "((b.espacio IS NULL OR b.espacio.espacioId = :espacioId)) AND " +
           "(b.fechaInicio <= :fechaFin AND b.fechaFin >= :fechaInicio)")
    List<BloqueoHorario> findBloqueosEnRango(
        @Param("espacioId") Long espacioId,
        @Param("fechaInicio") LocalDateTime fechaInicio,
        @Param("fechaFin") LocalDateTime fechaFin
    );

    // Buscar bloqueos por rango de fechas
    @Query("SELECT b FROM BloqueoHorario b WHERE " +
           "b.fechaInicio BETWEEN :fechaInicio AND :fechaFin OR " +
           "b.fechaFin BETWEEN :fechaInicio AND :fechaFin")
    List<BloqueoHorario> findBloqueosPorRangoFechas(
        @Param("fechaInicio") LocalDate fechaInicio,
        @Param("fechaFin") LocalDate fechaFin
    );

    // Buscar bloqueos globales (sin espacio específico)
    List<BloqueoHorario> findByEspacioIsNull();

    // Buscar bloqueos por espacio
    List<BloqueoHorario> findByEspacioEspacioId(Long espacioId);

    // Verificar si una fecha/hora específica está bloqueada
    @Query("SELECT COUNT(b) > 0 FROM BloqueoHorario b WHERE " +
           "((b.espacio IS NULL OR b.espacio.espacioId = :espacioId) AND " +
           "b.fechaInicio <= :fecha AND b.fechaFin >= :fecha AND " +
           "(:todoElDia = true OR " +
           "(:hora IS NULL OR (b.todoElDia = true OR " +
           "(:hora BETWEEN TIME(b.fechaInicio) AND TIME(b.fechaFin))))))")
    boolean isHorarioBloqueado(
        @Param("espacioId") Long espacioId,
        @Param("fecha") LocalDate fecha,
        @Param("hora") LocalTime hora,
        @Param("todoElDia") boolean todoElDia
    );
}
