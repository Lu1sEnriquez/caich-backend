package com.api_caich.enums;

public enum EstadoFinanciero {
    PENDIENTE, // Saldo pendiente total
    PAGO_PARCIAL, // Abonaron algo
    EN_REVISION, // Subieron comprobante, falta validar
    PAGADO, // Saldo cubierto al 100%
    REEMBOLSADO, // Se devolvió el dinero
    REEMBOLSADO_PARCIAL // Se autorizó una devolución menor al importe solicitado
}
