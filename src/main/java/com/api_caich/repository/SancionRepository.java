package com.api_caich.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.api_caich.entity.Sancion;
import com.api_caich.enums.EstadoSancion;

@Repository
public interface SancionRepository extends JpaRepository<Sancion, Long> {

    List<Sancion> findByAlumno_UsuarioId(Long alumnoId);

    List<Sancion> findByAlumno_UsuarioIdAndEstado(Long alumnoId, EstadoSancion estado);

    List<Sancion> findByEstado(EstadoSancion estado);

    @Query("SELECT s FROM Sancion s WHERE s.alumno.usuarioId = :alumnoId " +
           "AND s.estado = :estado " +
           "AND (s.fechaFin IS NULL OR s.fechaFin >= :fecha)")
    List<Sancion> findSancionesActivasParaAlumno(
        @Param("alumnoId") Long alumnoId,
        @Param("estado") EstadoSancion estado,
        @Param("fecha") LocalDate fecha
    );
}
