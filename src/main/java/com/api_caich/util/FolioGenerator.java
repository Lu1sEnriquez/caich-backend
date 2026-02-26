package com.api_caich.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

import com.api_caich.repository.TicketRepository;

@Component
public class FolioGenerator {

    private final TicketRepository ticketRepository;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    public FolioGenerator(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    public String generarFolioTicket() {
        String timestamp = LocalDateTime.now().format(FORMATTER);

        // Consultamos el máximo actual en la BD justo antes de generar el nuevo
        int nextSequence = ticketRepository.findMaxSequenceForCurrentMonth() + 1;

        return String.format("TKT-%s-%04d", timestamp, nextSequence);
    }

}