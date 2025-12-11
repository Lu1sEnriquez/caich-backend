package com.api_caich.repository;

import com.api_caich.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    Optional<Producto> findByCodigo(String codigo);

    List<Producto> findByCategoria(String categoria);

    List<Producto> findByEstaActivoTrue();

    @Query("SELECT p FROM Producto p WHERE p.stock <= p.stockMinimo AND p.estaActivo = true")
    List<Producto> findProductosStockBajo();

    @Query("SELECT p FROM Producto p WHERE p.stock = 0 AND p.estaActivo = true")
    List<Producto> findProductosSinStock();

    @Query("SELECT p FROM Producto p WHERE p.esPrestable = true AND p.estaActivo = true")
    List<Producto> findProductosPrestables();
}