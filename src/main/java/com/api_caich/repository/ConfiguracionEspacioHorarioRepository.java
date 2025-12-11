package com.api_caich.repository;

import com.api_caich.entity.ConfiguracionEspacioHorario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConfiguracionEspacioHorarioRepository extends JpaRepository<ConfiguracionEspacioHorario, Long> {

    // Buscar configuraciones por espacio y fecha
    List<ConfiguracionEspacioHorario> findByEspacioEspacioIdAndFecha(Long espacioId, LocalDate fecha);

    // Buscar configuraciones por espacio
    List<ConfiguracionEspacioHorario> findByEspacioEspacioId(Long espacioId);

    // Buscar configuración específica por espacio, fecha y hora
    Optional<ConfiguracionEspacioHorario> findByEspacioEspacioIdAndFechaAndHoraInicio(
            Long espacioId, LocalDate fecha, LocalTime horaInicio);

    // Verificar si existe una configuración para espacio, fecha y hora
    boolean existsByEspacioEspacioIdAndFechaAndHoraInicio(
            Long espacioId, LocalDate fecha, LocalTime horaInicio);

    // Buscar configuraciones por fecha
    List<ConfiguracionEspacioHorario> findByFecha(LocalDate fecha);

    // Buscar configuraciones por rango de fechas
    List<ConfiguracionEspacioHorario> findByFechaBetween(LocalDate fechaInicio, LocalDate fechaFin);

    // Buscar configuraciones deshabilitadas por espacio y fecha
    List<ConfiguracionEspacioHorario> findByEspacioEspacioIdAndFechaAndEstaDisponibleFalse(
            Long espacioId, LocalDate fecha);

    // Eliminar configuraciones por espacio y fecha
    @Modifying
    @Query("DELETE FROM ConfiguracionEspacioHorario c WHERE c.espacio.espacioId = :espacioId AND c.fecha = :fecha")
    void deleteByEspacioEspacioIdAndFecha(@Param("espacioId") Long espacioId, @Param("fecha") LocalDate fecha);

    // Verificar disponibilidad de un horario específico
    @Query("SELECT COUNT(c) = 0 FROM ConfiguracionEspacioHorario c WHERE " +
            "c.espacio.espacioId = :espacioId AND " +
            "c.fecha = :fecha AND " +
            "c.estaDisponible = false AND " +
            "(:hora BETWEEN c.horaInicio AND COALESCE(c.horaFin, c.horaInicio.plusHours(1)))")
    boolean isHorarioDisponible(@Param("espacioId") Long espacioId,
            @Param("fecha") LocalDate fecha,
            @Param("hora") LocalTime hora);

    // Buscar conflictos de horario
    @Query("SELECT c FROM ConfiguracionEspacioHorario c WHERE " +
            "c.espacio.espacioId = :espacioId AND " +
            "c.fecha = :fecha AND " +
            "c.estaDisponible = false AND " +
            "((c.horaInicio <= :horaFin AND COALESCE(c.horaFin, c.horaInicio.plusHours(1)) >= :horaInicio))")
    List<ConfiguracionEspacioHorario> findConflictingHorarios(
            @Param("espacioId") Long espacioId,
            @Param("fecha") LocalDate fecha,
            @Param("horaInicio") LocalTime horaInicio,
            @Param("horaFin") LocalTime horaFin);
}
