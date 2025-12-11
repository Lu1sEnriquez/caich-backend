package com.api_caich.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "Productos")
public class Producto extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ProductoId")
    private Long productoId;

    @Column(name = "Codigo", nullable = false, unique = true, length = 50)
    private String codigo;

    @Column(name = "Nombre", nullable = false, length = 200)
    private String nombre;

    @Column(name = "Descripcion", columnDefinition = "NVARCHAR(MAX)")
    private String descripcion;

    @Column(name = "Categoria", length = 50)
    private String categoria;

    @Column(name = "Precio", nullable = false, precision = 10, scale = 2)
    private BigDecimal precio = BigDecimal.ZERO;

    @Column(name = "Stock", nullable = false)
    private Integer stock = 0;

    @Column(name = "StockMinimo", nullable = false)
    private Integer stockMinimo = 5;

    @Column(name = "EsVendible", nullable = false)
    private Boolean esVendible = true;

    @Column(name = "EsPrestable", nullable = false)
    private Boolean esPrestable = false;

    @Column(name = "EstaActivo", nullable = false)
    private Boolean estaActivo = true;

    @OneToMany(mappedBy = "producto")
    private List<TicketDetalle> ticketDetalles;

    @OneToMany(mappedBy = "producto")
    private List<PrestamoMaterial> prestamos;

    @OneToMany(mappedBy = "producto")
    private List<MovimientoInventario> movimientos;
}