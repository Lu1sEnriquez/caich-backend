package com.api_caich.dto.pago;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SincronizacionAjustesRequestDTO {
    @Builder.Default
    private List<SincronizacionAjusteRegistroDTO> ajustes = new ArrayList<>();
}
