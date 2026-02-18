package com.api_caich.service;

import com.api_caich.entity.Pago;
import com.api_caich.exception.BusinessException;
import com.api_caich.exception.ResourceNotFoundException;
import com.api_caich.repository.PagoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComprobanteService {

    private final PagoRepository pagoRepository;
    private final FileStorageService fileStorageService;
    private final AuditoriaService auditoriaService;

    @Transactional
    public void subirComprobanteIngreso(Long pagoId, MultipartFile archivo, Long usuarioId) {
        Pago pago = pagoRepository.findById(pagoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado"));

        try {
            // Almacenar comprobante
            String comprobanteUrl = fileStorageService.almacenarComprobante(archivo, pagoId);
            
            // Actualizar pago con URL
            pago.setComprobanteUrl(comprobanteUrl);
            pagoRepository.save(pago);

            log.info("Comprobante de ingreso {} subido para pago {}", comprobanteUrl, pagoId);

            // Auditoría
            auditoriaService.registrarCambio("Pagos", pagoId, "Actualizar",
                    "Comprobante de ingreso subido", null, null, usuarioId, "SYSTEM");

        } catch (Exception e) {
            log.error("Error al subir comprobante para pago {}", pagoId, e);
            throw new BusinessException("Error al subir comprobante: " + e.getMessage());
        }
    }
}
