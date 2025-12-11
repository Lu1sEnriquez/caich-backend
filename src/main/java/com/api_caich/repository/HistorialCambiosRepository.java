package com.api_caich.repository;

import com.api_caich.entity.HistorialCambios;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface HistorialCambiosRepository extends JpaRepository<HistorialCambios, Long> {

    List<HistorialCambios> findByEntidadAndEntidadIdOrderByFechaCambioDesc(String entidad, Long entidadId);

    List<HistorialCambios> findByUsuarioUsuarioIdOrderByFechaCambioDesc(Long usuarioId);

    @Modifying
    @Query("DELETE FROM HistorialCambios h WHERE h.fechaCambio < :fechaLimite")
    int deleteByFechaCambioBefore(@Param("fechaLimite") LocalDateTime fechaLimite);
}