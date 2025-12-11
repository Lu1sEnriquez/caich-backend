package com.api_caich.repository;

import com.api_caich.entity.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    List<Notificacion> findByUsuarioUsuarioIdAndLeidaFalse(Long usuarioId);

    List<Notificacion> findByUsuarioUsuarioIdOrderByFechaCreacionDesc(Long usuarioId);

    @Query("SELECT n FROM Notificacion n WHERE n.usuario.usuarioId = :usuarioId AND n.leida = false ORDER BY n.fechaCreacion DESC")
    List<Notificacion> findByUsuarioUsuarioIdAndLeidaFalseOrderByFechaCreacionDesc(@Param("usuarioId") Long usuarioId);

    boolean existsByTipoAndReferenciaIdAndLeidaFalse(String tipo, Long referenciaId);

    @Modifying
    @Query("DELETE FROM Notificacion n WHERE n.leida = true AND n.fechaLeida < :fechaLimite")
    int deleteByLeidaTrueAndFechaLeidaBefore(@Param("fechaLimite") LocalDateTime fechaLimite);
}