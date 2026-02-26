package com.api_caich.repository;

import com.api_caich.entity.Configuracion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConfiguracionRepository extends JpaRepository<Configuracion, Integer> {
    @Query("SELECT c FROM Configuracion c WHERE c.clave = :clave AND c.activo = true")
    Optional<Configuracion> findByClave(@Param("clave") String clave);
}