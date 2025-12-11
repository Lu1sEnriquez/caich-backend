package com.api_caich.repository;

import com.api_caich.entity.CuentaBancaria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CuentaBancariaRepository extends JpaRepository<CuentaBancaria, Long> {

    List<CuentaBancaria> findByEstaActivaTrue();

    List<CuentaBancaria> findByBanco(String banco);

    // Buscar cuentas por banco
    List<CuentaBancaria> findByBancoContainingIgnoreCase(String banco);

    // Buscar cuentas por titular
    List<CuentaBancaria> findByTitularContainingIgnoreCase(String titular);

    // Buscar cuentas por estado
    List<CuentaBancaria> findByEstaActiva(Boolean estaActiva);

    // Verificar si existe una cuenta con el mismo número
    boolean existsByNumeroCuenta(String numeroCuenta);

    // Verificar si existe una cuenta con el mismo número excluyendo una cuenta
    // específica
    boolean existsByNumeroCuentaAndCuentaIdNot(String numeroCuenta, Long cuentaId);

    // Búsqueda general (mantenemos Pageable para el método del servicio)
    @Query("SELECT c FROM CuentaBancaria c WHERE " +
            "LOWER(c.banco) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.numeroCuenta) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.titular) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.clabe) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<CuentaBancaria> searchCuentas(@Param("search") String search, Pageable pageable);

    // Contar cuentas activas
    long countByEstaActivaTrue();

}