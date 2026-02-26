package com.api_caich.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.api_caich.entity.Espacio;

@Repository
public interface EspacioRepository extends JpaRepository<Espacio, Long> {

    List<Espacio> findByEstaActivoTrue();

    @Query("SELECT e FROM Espacio e WHERE e.estaActivo = true AND e.capacidad >= :capacidadMinima")
    List<Espacio> findEspaciosDisponiblesPorCapacidad(Integer capacidadMinima);

    boolean existsByNombre(String nombre);

    boolean existsByNombreAndEspacioIdNot(String nombre, Long espacioId);

    long countByEstaActivoTrue();
}