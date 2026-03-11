package com.api_caich.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

import com.api_caich.repository.TicketRepository;

@Component
public class FolioGenerator {

    private final TicketRepository ticketRepository;
    private static final DateTimeFormatter YEAR_FORMATTER = DateTimeFormatter.ofPattern("yy");

    public FolioGenerator(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    /**
     * Genera folio con formato YYNNNN (ej: 260001).
     * Secuencia de 4 dígitos que se reinicia cada año (máx 9999 tickets/año).
     */
    public String generarFolioTicket() {
        String yearPrefix = LocalDateTime.now().format(YEAR_FORMATTER);
        int nextSequence = ticketRepository.findMaxSequenceForCurrentYear() + 1;
        return String.format("%s%04d", yearPrefix, nextSequence);
    }
}