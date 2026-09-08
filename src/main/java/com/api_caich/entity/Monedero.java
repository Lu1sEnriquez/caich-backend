package com.api_caich.entity;

import java.math.BigDecimal;

import com.api_caich.enums.Role;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

/**
 * Monedero virtual del alumno.
 * El campo "saldo" usa locking optimista (@Version) para prevenir
 * condiciones de carrera en escenarios concurrentes (doble-descuento, etc.).
 */
@Getter
@Setter
@Entity
@Table(name = "Monederos")
public class Monedero extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MonederoId")
    private Long monederoId;

    /**
     * Relación 1-a-1 con Usuario (solo alumnos).
     * Se valida en el servicio que rol == ALUMNO antes de crear/acceder.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AlumnoId", nullable = false, unique = true)
    private Usuario alumno;

    @Column(name = "Saldo", nullable = false, precision = 10, scale = 2)
    private BigDecimal saldo = BigDecimal.ZERO;

    /**
     * Control de concurrencia optimista: JPA incrementa este valor
     * en cada UPDATE. Si dos transacciones leen el mismo registro y
     * la primera graba antes, la segunda recibe OptimisticLockException
     * en lugar de sobreescribir silenciosamente.
     */
    @Version
    @Column(name = "Version", nullable = false)
    private Long version = 0L;
}
