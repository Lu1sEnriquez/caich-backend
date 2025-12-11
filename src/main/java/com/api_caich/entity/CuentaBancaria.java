package com.api_caich.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

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

    @OneToMany(mappedBy = "cuentaDestino")
    private List<Ticket> tickets;
}