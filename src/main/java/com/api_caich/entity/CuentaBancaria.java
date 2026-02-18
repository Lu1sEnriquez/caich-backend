package com.api_caich.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "CuentasBancarias")
public class CuentaBancaria extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CuentaId")
    private Long cuentaId;

    @Column(name = "Banco", nullable = false, length = 100)
    private String banco;

    @Column(name = "NumeroCuenta", nullable = false, length = 50)
    private String numeroCuenta;

    @Column(name = "CLABE", length = 18)
    private String clabe;

    @Column(name = "Titular", nullable = false, length = 200)
    private String titular;

    @Column(name = "EstaActiva", nullable = false)
    private Boolean estaActiva = true;

}
