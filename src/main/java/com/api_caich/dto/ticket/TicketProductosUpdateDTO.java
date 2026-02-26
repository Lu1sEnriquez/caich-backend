package com.api_caich.dto.ticket;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import lombok.Data;

@Data
public class TicketProductosUpdateDTO {

    @Valid
    private List<TicketVentaDTO> ventas = new ArrayList<>();

    @Valid
    private List<TicketPrestamoDTO> prestamos = new ArrayList<>();
}
