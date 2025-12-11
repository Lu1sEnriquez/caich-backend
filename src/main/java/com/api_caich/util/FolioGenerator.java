package com.api_caich.util;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class FolioGenerator {
    
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");
    private static final AtomicInteger SEQUENCE = new AtomicInteger(1);
    
    public static String generarFolioTicket() {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        int sequence = SEQUENCE.getAndIncrement();
        
        // Reset sequence if it gets too high
        if (sequence > 9999) {
            SEQUENCE.set(1);
        }
        
        return String.format("TKT-%s-%04d", timestamp, sequence);
    }
    
    public static String generarFolioMovimiento() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int sequence = SEQUENCE.getAndIncrement();
        
        return String.format("MOV-%s-%04d", timestamp, sequence);
    }
}