package com.api_caich.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.api_caich.enums.EstadoFinanciero;
import com.api_caich.enums.EstadoTicket;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "Tickets")
public class Ticket extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TicketId")
    private Long ticketId;

    @Column(name = "Folio", nullable = false, unique = true, length = 50)
    private String folio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CreadoPor", nullable = false)
    private Usuario creadoPor; // Quién hizo el ticket en el sistema

    /** Persona a quien se vende o presta; puede ser distinta de quien lo capturó. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CompradorId")
    private Usuario comprador;

    // --- ESTATUS ---
    @Enumerated(EnumType.STRING)
    @Column(name = "EstadoTicket", nullable = false)
    private EstadoTicket estadoTicket = EstadoTicket.BORRADOR;

    @Enumerated(EnumType.STRING)
    @Column(name = "EstadoFinanciero", nullable = false)
    private EstadoFinanciero estadoFinanciero = EstadoFinanciero.PENDIENTE;

    // --- LISTAS MODULARES (El corazón de la arquitectura) ---
    // 1. Ventas
    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TicketVenta> ventas = new ArrayList<>();

    // 2. Cita (Opcional, 1 por ticket para simplificar)
    @OneToOne(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    private TicketCita cita;

    // 3. Espacios (Renta de cubícuos)
    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TicketEspacio> espacios = new ArrayList<>();

    // 4. Préstamos
    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TicketPrestamo> prestamos = new ArrayList<>();

    // 5. Historial de Pagos
    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL)
    private List<Pago> pagos = new ArrayList<>();

    // --- TOTALES Y SALDOS ---
    @Column(name = "CostoAdicional", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoAdicional = BigDecimal.ZERO;

    @Column(name = "MotivoCostoAdicional")
    private String motivoCostoAdicional;

    // Suma de todo lo que se debe cobrar
    @Column(name = "MontoTotal", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoTotal = BigDecimal.ZERO;

    // Suma de los pagos APROBADOS
    @Column(name = "MontoPagado", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoPagado = BigDecimal.ZERO;

    // --- MÉTODOS DE AYUDA ---
    /**
     * Calcula cuánto debe el usuario restando lo pagado del total.
     */
    public BigDecimal getSaldoPendiente() {
        return montoTotal.subtract(montoPagado);
    }

    /**
     * Recalcula el total del ticket sumando todas las listas. Útil para llamar
     * antes de guardar.
     */
    public void recalcularTotal() {
        BigDecimal totalVentas = ventas.stream()
                .map(TicketVenta::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCita = (cita != null) ? cita.getPrecioCobrado() : BigDecimal.ZERO;

        BigDecimal totalEspacios = espacios.stream()
                .map(TicketEspacio::getCostoCobrado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.montoTotal = totalVentas
                .add(totalCita)
                .add(totalEspacios)
                .add(costoAdicional != null ? costoAdicional : BigDecimal.ZERO);
    }
}
