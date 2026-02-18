package com.api_caich.dto.ticket;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.api_caich.enums.EstadoFinanciero;
import com.api_caich.enums.EstadoTicket;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TicketRequestDTO {

    @NotNull(message = "El usuario creador es requerido")
    private Long creadoPorId;

    private EstadoTicket estadoTicket = EstadoTicket.BORRADOR;

    private EstadoFinanciero estadoFinanciero = EstadoFinanciero.PENDIENTE;

    // Listas modulares opcionales
    @Valid
    private List<TicketVentaDTO> ventas = new ArrayList<>();

    @Valid
    private TicketCitaDTO cita;

    @Valid
    private List<TicketEspacioDTO> espacios = new ArrayList<>();

    @Valid
    private List<TicketPrestamoDTO> prestamos = new ArrayList<>();

    @DecimalMin(value = "0.0", message = "El costo adicional debe ser mayor o igual a 0")
    private BigDecimal costoAdicional = BigDecimal.ZERO;

    private String motivoCostoAdicional;
}
