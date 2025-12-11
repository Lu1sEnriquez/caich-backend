package com.api_caich.repository;

import com.api_caich.entity.PrestamoMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface PrestamoMaterialRepository extends JpaRepository<PrestamoMaterial, Long> {

    List<PrestamoMaterial> findByAlumnoUsuarioId(Long alumnoId);

    List<PrestamoMaterial> findByEstadoPrestamo(String estadoPrestamo);

    Page<PrestamoMaterial> findByAlumnoUsuarioId(Long alumnoId, Pageable pageable);

    @Query("SELECT p FROM PrestamoMaterial p WHERE p.alumno.usuarioId = :alumnoId AND p.estadoPrestamo IN :estados")
    List<PrestamoMaterial> findByAlumnoUsuarioIdAndEstadoPrestamoIn(@Param("alumnoId") Long alumnoId,
            @Param("estados") List<String> estados);

    List<PrestamoMaterial> findByEstadoPrestamoAndFechaDevolucionEstimadaBefore(String estadoPrestamo,
            LocalDateTime fecha);

    Long countByEstadoPrestamo(String estadoPrestamo);
}