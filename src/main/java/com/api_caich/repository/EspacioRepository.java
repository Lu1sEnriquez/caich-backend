package com.api_caich.repository;

import com.api_caich.entity.Espacio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EspacioRepository extends JpaRepository<Espacio, Long> {

    List<Espacio> findByEstaActivoTrue();

    List<Espacio> findByTipo(String tipo);

    @Query("SELECT e FROM Espacio e WHERE e.estaActivo = true AND e.capacidad >= :capacidadMinima")
    List<Espacio> findEspaciosDisponiblesPorCapacidad(Integer capacidadMinima);

    boolean existsByNombre(String nombre);

    boolean existsByNombreAndEspacioIdNot(String nombre, Long espacioId);

    long countByEstaActivoTrue();
}