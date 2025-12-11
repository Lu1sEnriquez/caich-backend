package com.api_caich.dto.horario;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracionHorariosDTO {
    private Long configId;

    @NotNull
    private Long espacioId;
    private String espacioNombre;

    @NotNull
    private LocalDate fecha;

    // Lista de horas deshabilitadas en formato ["09:00", "10:00", "11:00"]
    @NotNull
    private List<String> horasDeshabilitadas;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime fechaCreacion;
}
