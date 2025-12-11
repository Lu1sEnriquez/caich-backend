package com.api_caich.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LogoutRequestDTO {
    @NotBlank(message = "Refresh token es requerido")
    private String refreshToken;
}