package com.api_caich.entity;

import java.math.BigDecimal; // <--- 1. IMPORTANTE: Agrega este import
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Productos")
@JsonIgnoreProperties(ignoreUnknown = true)
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

    // --- RELACIONES CORREGIDAS ---
    // Nota: TicketDetalle y PrestamoMaterial han sido removidas en favor de la nueva arquitectura modular
    // TicketVenta y TicketPrestamo ahora manejan las relaciones con Producto
    @OneToMany(mappedBy = "producto")
    @JsonIgnore
    private List<MovimientoInventario> movimientos;

    public String getEstadoStock() {
        if (this.stock <= 0) {
            return "AGOTADO";
        }
        if (this.stock <= this.stockMinimo) {
            return "CRÍTICO";
        }
        return "OK";
    }
}
