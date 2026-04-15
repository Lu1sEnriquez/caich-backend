package com.api_caich.repository;

import com.api_caich.entity.Producto;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    Optional<Producto> findByCodigo(String codigo);

    List<Producto> findByCategoria(String categoria);

    List<Producto> findByEstaActivoTrue();

    // En ProductoRepository.java
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Producto p WHERE p.productoId = :id")
    Optional<Producto> findByIdForUpdate(Long id);


    @Query("SELECT p FROM Producto p WHERE p.stock <= p.stockMinimo AND p.estaActivo = true")
    List<Producto> findProductosStockBajo();

    @Query("SELECT p FROM Producto p WHERE p.stock = 0 AND p.estaActivo = true")
    List<Producto> findProductosSinStock();

    @Query("SELECT p FROM Producto p WHERE p.esPrestable = true AND p.estaActivo = true")
    List<Producto> findProductosPrestables();

    // Opcional: Búsqueda por nombre para el buscador de la UI que mostraste
    Page<Producto> findByNombreContainingIgnoreCase(String nombre, Pageable pageable);

    @Query("SELECT p FROM Producto p WHERE " +
           "(CAST(:nombre AS string) IS NULL OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', CAST(:nombre AS string), '%'))) AND " +
           "(CAST(:categoria AS string) IS NULL OR p.categoria = CAST(:categoria AS string)) AND " +
           "(:estaActivo IS NULL OR p.estaActivo = :estaActivo) AND " +
           "(:esPrestable IS NULL OR p.esPrestable = :esPrestable) AND " +
           "(:esVendible IS NULL OR p.esVendible = :esVendible)")
    Page<Producto> findAllConFiltros(
            String nombre, 
            String categoria, 
            Boolean estaActivo, 
            Boolean esPrestable, 
            Boolean esVendible, 
            Pageable pageable);
            
}