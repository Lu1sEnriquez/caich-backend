package com.api_caich.dto.usuario;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FotoPerfilDTO {
    private String fotoUrl;
    private String mensaje;
}
