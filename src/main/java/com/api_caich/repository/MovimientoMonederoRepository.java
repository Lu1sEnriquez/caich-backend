package com.api_caich.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.api_caich.entity.MovimientoMonedero;

@Repository
public interface MovimientoMonederoRepository extends JpaRepository<MovimientoMonedero, Long> {

    Page<MovimientoMonedero> findByMonedero_MonederoId(Long monederoId, Pageable pageable);

    /** Movimientos de un alumno específico dentro de un rango de fechas (para reportes). */
    @Query("""
        SELECT m FROM MovimientoMonedero m
        WHERE m.monedero.alumno.usuarioId = :alumnoId
          AND m.fechaMovimiento BETWEEN :desde AND :hasta
        ORDER BY m.fechaMovimiento DESC
        """)
    List<MovimientoMonedero> findByAlumnoAndRangoFechas(
            @Param("alumnoId") Long alumnoId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    /** Todos los movimientos dentro de un rango de fechas (export admin). */
    @Query("""
        SELECT m FROM MovimientoMonedero m
        WHERE m.fechaMovimiento BETWEEN :desde AND :hasta
        ORDER BY m.fechaMovimiento DESC
        """)
    List<MovimientoMonedero> findByRangoFechas(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);
}
