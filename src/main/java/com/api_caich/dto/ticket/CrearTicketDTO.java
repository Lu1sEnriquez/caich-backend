package com.api_caich.dto.ticket;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CrearTicketDTO {

    @NotNull(message = "Paciente ID is required")
    private Long pacienteId;

    @NotNull(message = "Terapeuta ID is required")
    private Long terapeutaId;

    private Long espacioId;

    @NotNull(message = "Cuenta destino ID is required")
    private Long cuentaDestinoId;

    @NotNull(message = "Fecha is required")
    @Future(message = "Fecha must be in the future")
    private LocalDateTime fecha;

    @Min(value = 1, message = "Duracion must be at least 1 minute")
    private Integer duracion;

    @NotBlank(message = "Materia is required")
    private String materia;

    private String conceptoIngreso;
    private String conceptoTransferencia;

    @NotNull(message = "Monto pagado is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Monto pagado must be greater than 0")
    private BigDecimal montoPagado;

    private String celular;

    @DecimalMin(value = "0.0", message = "Costo espacio cannot be negative")
    private BigDecimal costoEspacio = BigDecimal.ZERO;

    @DecimalMin(value = "0.0", message = "Costo adicional cannot be negative")
    private BigDecimal costoAdicional = BigDecimal.ZERO;

    @NotEmpty(message = "At least one product is required")
    private List<ProductoDetalleDTO> productos;
}