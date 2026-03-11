package com.api_caich.service;

import java.io.StringReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.api_caich.dto.pago.ErrorSincronizacionDTO;
import com.api_caich.dto.pago.PagoSincronizacionDTO;
import com.api_caich.dto.pago.SincronizacionAjusteRegistroDTO;
import com.api_caich.dto.pago.SincronizacionPreviewDTO;
import com.api_caich.dto.pago.SincronizacionResultadoDTO;
import com.api_caich.entity.Configuracion;
import com.api_caich.entity.Ticket;
import com.api_caich.enums.MetodoPago;
import com.api_caich.exception.BusinessException;
import com.api_caich.repository.ConfiguracionRepository;
import com.api_caich.repository.PagoRepository;
import com.api_caich.repository.TicketRepository;
import com.opencsv.CSVReader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleFormsSyncService {

    private final PagoService pagoService;
    private final PagoRepository pagoRepository;
    private final TicketRepository ticketRepository;
    private final RestTemplate restTemplate;
    private final ConfiguracionRepository configRepository;

    private static final Pattern DRIVE_FILE_ID_PATTERN = Pattern.compile(
            "(?:(?:/file/d/)|(?:[?&]id=))([a-zA-Z0-9_-]{25,})"
    );
    
    private static final Pattern DRIVE_FILE_ID_PATTERN_ALTERNATE = Pattern.compile(
            "[?&]id=([a-zA-Z0-9_-]+)"
    );

    /**
     * Previsualiza la sincronización sin guardar nada en la BD
     * Valida todos los registros y retorna qué se va a sincronizar
     */
    public SincronizacionPreviewDTO previsualizarSincronizacion() {
        return previsualizarSincronizacionConAjustes(Collections.emptyList());
    }

    /**
     * Previsualiza la sincronización aplicando ajustes temporales del review.
     */
    public SincronizacionPreviewDTO previsualizarSincronizacionConAjustes(List<SincronizacionAjusteRegistroDTO> ajustes) {
        try {
            log.info("Iniciando previsualización de sincronización desde Google Sheets...");

            String csvContent = descargarCSV();
            Map<String, SincronizacionAjusteRegistroDTO> ajustesPorMarcaTemporal = indexarAjustes(ajustes);
            
            try (CSVReader reader = new CSVReader(new StringReader(csvContent))) {
                List<String[]> filas = reader.readAll();

                List<PagoSincronizacionDTO> pagosValidos = new ArrayList<>();
                List<PagoSincronizacionDTO> pagosConError = new ArrayList<>();
                int totalDuplicados = 0;

                for (int i = 1; i < filas.size(); i++) {
                    String[] filaOriginal = filas.get(i);
                    String[] fila = aplicarAjustesFila(filaOriginal, ajustesPorMarcaTemporal);

                    if (fila.length < 9) {
                        PagoSincronizacionDTO error = PagoSincronizacionDTO.builder()
                                .numeroFila(i + 1)
                                .esValido(false)
                                .mensajeError("Fila incompleta - se esperan al menos 9 columnas")
                                .build();
                        pagosConError.add(error);
                        continue;
                    }

                    PagoSincronizacionDTO resultado = validarRegistro(fila, i + 1);
                    
                    if (resultado.getEsDuplicado()) {
                        totalDuplicados++;
                    } else if (resultado.getEsValido()) {
                        pagosValidos.add(resultado);
                    } else {
                        pagosConError.add(resultado);
                    }
                }

                return SincronizacionPreviewDTO.builder()
                        .totalRegistros(filas.size() - 1)
                        .pagosValidos(pagosValidos)
                        .pagosConError(pagosConError)
                        .totalValidos(pagosValidos.size())
                        .totalErrores(pagosConError.size())
                        .totalDuplicados(totalDuplicados)
                        .build();
            }

        } catch (Exception e) {
            log.error("Error durante la previsualización: ", e);
            throw new BusinessException("Error al previsualizar la sincronización: " + e.getMessage());
        }
    }

    /**
     * Confirma y ejecuta la sincronización de los pagos válidos
     */
    @Transactional
    public SincronizacionResultadoDTO confirmarSincronizacion(Long adminId) {
        return ejecutarSincronizacion(previsualizarSincronizacion(), adminId);
    }

    /**
     * Confirma y ejecuta la sincronización aplicando ajustes temporales del review.
     */
    @Transactional
    public SincronizacionResultadoDTO confirmarSincronizacionConAjustes(
            Long adminId,
            List<SincronizacionAjusteRegistroDTO> ajustes
    ) {
        return ejecutarSincronizacion(previsualizarSincronizacionConAjustes(ajustes), adminId);
    }

    private SincronizacionResultadoDTO ejecutarSincronizacion(SincronizacionPreviewDTO preview, Long adminId) {
        try {
            log.info("Iniciando sincronización confirmada desde Google Sheets...");
            
            List<String> foliosSincronizados = new ArrayList<>();
            List<ErrorSincronizacionDTO> errores = new ArrayList<>();

            // Sincronizar solo los válidos
            for (PagoSincronizacionDTO pago : preview.getPagosValidos()) {
                try {
                    log.info("Sincronizando folio: {}", pago.getFolio());
                    
                    String syncKey = "[GFORMS:" + pago.getMarcaTemporal() + "]";
                    String notas = syncKey + " Reportado por: " + pago.getNombrePaciente();
                    
                    pagoService.registrarPagoConComprobante(
                            pago.getTicketId(),
                            pago.getMonto(),
                            pago.getMetodoPago(),
                            pago.getReferencia(),
                            notas,
                            pago.getComprobanteUrl(),
                            adminId
                    );
                    
                    foliosSincronizados.add(pago.getFolio());
                    
                } catch (Exception e) {
                    log.error("Error al sincronizar folio {}: {}", pago.getFolio(), e.getMessage());
                    errores.add(ErrorSincronizacionDTO.builder()
                            .folio(pago.getFolio())
                            .numeroFila(pago.getNumeroFila())
                            .error(e.getMessage())
                            .build());
                }
            }

            String mensaje = String.format(
                    "Sincronización completada: %d exitosos, %d fallidos, %d con errores previos, %d duplicados",
                    foliosSincronizados.size(),
                    errores.size(),
                    preview.getTotalErrores(),
                    preview.getTotalDuplicados()
            );

            log.info(mensaje);

            return SincronizacionResultadoDTO.builder()
                    .totalSincronizados(foliosSincronizados.size())
                    .foliosSincronizados(foliosSincronizados)
                    .totalFallidos(errores.size())
                    .errores(errores)
                    .mensaje(mensaje)
                    .build();

        } catch (Exception e) {
            log.error("Error crítico durante la sincronización confirmada: ", e);
            throw new BusinessException("Error al ejecutar la sincronización: " + e.getMessage());
        }
    }

    private Map<String, SincronizacionAjusteRegistroDTO> indexarAjustes(List<SincronizacionAjusteRegistroDTO> ajustes) {
        Map<String, SincronizacionAjusteRegistroDTO> ajustesPorMarcaTemporal = new HashMap<>();
        if (ajustes == null) {
            return ajustesPorMarcaTemporal;
        }

        for (SincronizacionAjusteRegistroDTO ajuste : ajustes) {
            if (ajuste == null || ajuste.getMarcaTemporal() == null) {
                continue;
            }
            String marcaTemporal = ajuste.getMarcaTemporal().trim();
            if (!marcaTemporal.isEmpty()) {
                ajustesPorMarcaTemporal.put(marcaTemporal, ajuste);
            }
        }

        return ajustesPorMarcaTemporal;
    }

    private String[] aplicarAjustesFila(
            String[] filaOriginal,
            Map<String, SincronizacionAjusteRegistroDTO> ajustesPorMarcaTemporal
    ) {
        if (filaOriginal == null || filaOriginal.length == 0) {
            return filaOriginal;
        }

        String[] fila = Arrays.copyOf(filaOriginal, filaOriginal.length);
        String marcaTemporal = fila[0] == null ? "" : fila[0].trim();
        if (marcaTemporal.isEmpty()) {
            return fila;
        }

        SincronizacionAjusteRegistroDTO ajuste = ajustesPorMarcaTemporal.get(marcaTemporal);
        if (ajuste == null) {
            return fila;
        }

        // Aplicar todos los ajustes disponibles
        if (ajuste.getEmail() != null) {
            fila[1] = ajuste.getEmail().trim();
        }
        if (ajuste.getFolio() != null) {
            fila[2] = ajuste.getFolio().trim();
        }
        if (ajuste.getNombrePaciente() != null) {
            fila[3] = ajuste.getNombrePaciente().trim();
        }
        if (ajuste.getMonto() != null) {
            fila[4] = ajuste.getMonto().trim();
        }
        if (ajuste.getMetodoPago() != null) {
            fila[5] = ajuste.getMetodoPago().trim();
        }
        if (ajuste.getReferencia() != null) {
            fila[6] = ajuste.getReferencia().trim();
        }
        if (ajuste.getComprobanteUrl() != null) {
            fila[8] = ajuste.getComprobanteUrl().trim();
        }

        return fila;
    }

    /**
     * Valida un registro individual del CSV
     */
    private PagoSincronizacionDTO validarRegistro(String[] fila, int numeroFila) {
        String marcaTemporal = fila[0].trim();
        String email = fila[1].trim();
        String folio = fila[2].trim();
        String nombrePaciente = fila[3].trim();
        String montoRaw = fila[4].replaceAll("[^0-9.]", "");
        String metodoStr = fila[5].trim().toUpperCase();
        String referencia = fila[6].trim();
        String comprobanteUrlDrive = fila[8].trim();
        String notasAdicionales = fila.length > 9 ? fila[9].trim() : "";
        String comprobanteUrlLimpia = limpiarUrlDrive(comprobanteUrlDrive);
        
        // Parsear monto al inicio para que siempre esté disponible en la respuesta
        BigDecimal monto = null;
        try {
            if (!montoRaw.isEmpty()) {
                monto = new BigDecimal(montoRaw);
            }
        } catch (NumberFormatException e) {
            // Si no se puede parsear, monto quedará null y se reportará en validación
        }

        PagoSincronizacionDTO.PagoSincronizacionDTOBuilder builder = PagoSincronizacionDTO.builder()
                .numeroFila(numeroFila)
                .email(email)
                .folio(folio)
                .nombrePaciente(nombrePaciente)
                .monto(monto)
                .referencia(referencia)
                .metodoPago(metodoStr)
                .comprobanteUrl(comprobanteUrlLimpia)
                .notasAdicionales(notasAdicionales)
                .marcaTemporal(marcaTemporal)
                .esValido(true)
                .esDuplicado(false);

        // Validación 1: Formato de folio
        if (folio.isEmpty()) {
            return builder
                    .esValido(false)
                    .mensajeError("Folio vacío")
                    .build();
        }

        // Validación 2: Buscar ticket
        Optional<Ticket> ticketOpt = ticketRepository.findByFolio(folio);
        if (ticketOpt.isEmpty()) {
            return builder
                    .esValido(false)
                    .mensajeError("Ticket no encontrado con folio: " + folio)
                    .build();
        }

        Ticket ticket = ticketOpt.get();
        builder.ticketId(ticket.getTicketId())
                .montoTotalTicket(ticket.getMontoTotal())
                .montoPagadoTicket(ticket.getMontoPagado());

        // Validación 3: Monto
        if (monto == null) {
            return builder
                    .esValido(false)
                    .mensajeError("Formato de monto inválido: " + fila[4])
                    .build();
        }
        
        if (monto.compareTo(BigDecimal.ZERO) <= 0) {
            return builder
                    .esValido(false)
                    .mensajeError("El monto debe ser mayor a 0")
                    .build();
        }

        // Validación 4: Verificar duplicados por marca temporal (sync key)
        String syncKey = "[GFORMS:" + marcaTemporal + "]";
        boolean pagoYaExiste = pagoRepository.existsByNotasContaining(syncKey);
        
        if (pagoYaExiste) {
            return builder
                    .esValido(false)
                    .esDuplicado(true)
                    .mensajeError("Pago ya sincronizado anteriormente")
                    .build();
        }

        // Validación 5: Monto vs saldo pendiente
        BigDecimal saldoPendiente = ticket.getMontoTotal().subtract(ticket.getMontoPagado());
        builder.saldoPendienteTicket(saldoPendiente);
        
        if (monto.compareTo(saldoPendiente) > 0) {
            return builder
                    .esValido(false)
                    .mensajeError(String.format(
                            "El monto ($%.2f) excede el saldo pendiente ($%.2f)", 
                            monto, saldoPendiente))
                    .build();
        }

        // Validación 6: Método de pago válido
        try {
            MetodoPago.valueOf(metodoStr);
        } catch (IllegalArgumentException e) {
            return builder
                    .esValido(false)
                    .mensajeError("Método de pago inválido: " + metodoStr + ". Valores válidos: " + 
                            String.join(", ", getAllMetodoPagoNames()))
                    .build();
        }

        // Validación 7: URL de comprobante
        if (comprobanteUrlLimpia == null || comprobanteUrlLimpia.isEmpty()) {
            return builder
                    .esValido(false)
                    .mensajeError("URL de comprobante inválida o vacía")
                    .build();
        }

        // Todo válido
        return builder.build();
    }

    /**
     * Descarga el contenido CSV desde Google Sheets
     */
    private String descargarCSV() {
        Configuracion config = configRepository.findByClave("GOOGLE_FORMS_CSV_URL")
                .orElseThrow(() -> new BusinessException("No se encontró la URL de sincronización en la BD"));

        String csvUrl = config.getValor();

        String csvContent = restTemplate.getForObject(csvUrl, String.class);
        if (csvContent == null || csvContent.isEmpty()) {
            throw new BusinessException("El archivo CSV de Google está vacío o no es accesible.");
        }

        return csvContent;
    }

    /**
     * Obtiene los nombres de todos los métodos de pago válidos
     */
    private List<String> getAllMetodoPagoNames() {
        return java.util.Arrays.stream(MetodoPago.values())
                .map(Enum::name)
                .collect(Collectors.toList());
    }

    private String limpiarUrlDrive(String driveUrl) {
        if (driveUrl == null || driveUrl.trim().isEmpty()) {
            log.warn("URL de Drive vacía");
            return null;
        }

        driveUrl = driveUrl.trim();

        if (driveUrl.contains("/file/d/") && driveUrl.contains("/view")) {
            return driveUrl;
        }

        String fileId = extraerDriveFileId(driveUrl);
        if (fileId == null) {
            log.warn("No se pudo extraer ID. Usando URL original: {}", driveUrl);
            return driveUrl;
        }

        return String.format("https://drive.google.com/file/d/%s/view", fileId);
    }

    private String extraerDriveFileId(String driveUrl) {
        if (driveUrl == null || driveUrl.isEmpty()) {
            return null;
        }

        Matcher matcher = DRIVE_FILE_ID_PATTERN.matcher(driveUrl);
        if (matcher.find()) {
            String id = matcher.group(1);
            if (id != null && !id.isEmpty()) {
                return id;
            }
        }

        Matcher matcherAlt = DRIVE_FILE_ID_PATTERN_ALTERNATE.matcher(driveUrl);
        if (matcherAlt.find()) {
            String id = matcherAlt.group(1);
            if (id != null && !id.isEmpty()) {
                return id;
            }
        }

        int fileIndex = driveUrl.indexOf("/file/d/");
        if (fileIndex >= 0) {
            int start = fileIndex + 8;
            int end = driveUrl.indexOf("/", start);
            if (end < 0) end = driveUrl.indexOf("?", start);
            if (end < 0) end = driveUrl.length();
            
            if (start < end) {
                String id = driveUrl.substring(start, end).trim();
                if (!id.isEmpty()) return id;
            }
        }

        int idIndex = driveUrl.indexOf("id=");
        if (idIndex >= 0) {
            int start = idIndex + 3;
            int end = driveUrl.indexOf("&", start);
            if (end < 0) end = driveUrl.length();
            
            String id = driveUrl.substring(start, end).trim();
            if (!id.isEmpty()) return id;
        }

        return null;
    }
}
