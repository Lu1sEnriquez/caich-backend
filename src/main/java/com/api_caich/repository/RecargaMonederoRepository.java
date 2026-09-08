package com.api_caich.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.api_caich.entity.RecargaMonedero;
import com.api_caich.enums.EstadoRecargaMonedero;

@Repository
public interface RecargaMonederoRepository extends JpaRepository<RecargaMonedero, Long> {

    List<RecargaMonedero> findByAlumno_UsuarioIdOrderByFechaSolicitudDesc(Long alumnoId);

    List<RecargaMonedero> findByEstadoOrderByFechaSolicitudAsc(EstadoRecargaMonedero estado);

    Page<RecargaMonedero> findAllByOrderByFechaSolicitudDesc(Pageable pageable);

    /** Recargas dentro de un rango de fechas (para Excel admin). */
    @Query("""
            SELECT r FROM RecargaMonedero r
            WHERE r.fechaSolicitud BETWEEN :desde AND :hasta
            ORDER BY r.fechaSolicitud DESC
            """)
    List<RecargaMonedero> findByRangoFechas(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    /** ¿Cuántas recargas pendientes hay? (para badge en sidebar admin). */
    long countByEstado(EstadoRecargaMonedero estado);

    /** Recargas por método de pago y estado (para tesorería). */
    List<RecargaMonedero> findByMetodoPagoAndEstadoOrderByFechaSolicitudAsc(
            com.api_caich.enums.MetodoPago metodoPago,
            com.api_caich.enums.EstadoRecargaMonedero estado);
}
