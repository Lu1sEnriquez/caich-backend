package com.api_caich.dto.cita;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitaDTO {
    private Long ticketId; // Usamos TicketId como ID de cita
    private Long espacioId;
    private String espacioNombre;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime fecha;

    private Integer duracion; // en minutos
    private Long pacienteId;
    private String pacienteNombre;
    private Long terapeutaId;
    private String terapeutaNombre;
    private String materia; // Usamos materia como motivo
    private String modalidad; // "Presencial" | "Online"
    private String estadoTicket; // "Agendada" | "Completada" | "Cancelada" | "NoAsistio"
    private String notas;
    private LocalDateTime fechaCreacion;
}
