package com.api_caich.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
    @JoinColumn(name = "PacienteId", nullable = false)
    private Usuario paciente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TerapeutaId", nullable = false)
    private Usuario terapeuta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "EspacioId")
    private Espacio espacio;

    @Column(name = "Modalidad", length = 20)
    private String modalidad; // "Presencial", "Online", etc.

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CuentaDestinoId", nullable = false)
    private CuentaBancaria cuentaDestino;

    @Column(name = "Fecha", nullable = false)
    private LocalDateTime fecha;

    @Column(name = "Duracion")
    private Integer duracion;

    @Column(name = "Materia", length = 200)
    private String materia;

    @Column(name = "ConceptoIngreso", columnDefinition = "NVARCHAR(MAX)")
    private String conceptoIngreso;

    @Column(name = "ConceptoTransferencia", columnDefinition = "NVARCHAR(MAX)")
    private String conceptoTransferencia;

    @Column(name = "Notas", columnDefinition = "NVARCHAR(MAX)")
    private String notas;

    @Column(name = "MontoPagado", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoPagado;

    @Column(name = "Celular", length = 20)
    private String celular;

    @Column(name = "CostoEspacio", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoEspacio = BigDecimal.ZERO;

    @Column(name = "CostoMateriales", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoMateriales = BigDecimal.ZERO;

    @Column(name = "CostoAdicional", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoAdicional = BigDecimal.ZERO;

    @Column(name = "CostoTotal", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoTotal = BigDecimal.ZERO;

    @Column(name = "EstadoTicket", nullable = false, length = 20)
    private String estadoTicket = "Agendado";

    @Column(name = "EstadoPago", nullable = false, length = 20)
    private String estadoPago = "Pendiente";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CreadoPor")
    private Usuario creadoPor;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL)
    private List<TicketDetalle> detalles;

    @OneToMany(mappedBy = "ticket")
    private List<PrestamoMaterial> prestamos;
}