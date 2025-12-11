package com.api_caich.repository;

import com.api_caich.entity.MovimientoInventario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {

    List<MovimientoInventario> findByProductoProductoId(Long productoId);

    List<MovimientoInventario> findByTipoMovimiento(String tipoMovimiento);

    @Query("SELECT m FROM MovimientoInventario m WHERE m.fechaMovimiento BETWEEN :start AND :end")
    List<MovimientoInventario> findByFechaMovimientoBetween(@Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT m FROM MovimientoInventario m WHERE m.producto.productoId = :productoId ORDER BY m.fechaMovimiento DESC")
    List<MovimientoInventario> findUltimosMovimientosByProducto(@Param("productoId") Long productoId,
            org.springframework.data.domain.Pageable pageable);
}