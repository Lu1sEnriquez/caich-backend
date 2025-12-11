package com.api_caich.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponseDTO {
    private String accessToken;
    private String refreshToken;
    private String type;
    private Long usuarioId;
    private String nombreCompleto;
    private String email;
    private String rol;

    @Builder.Default
    private Long expiresIn = 86400L; // 24 hours in seconds
}
