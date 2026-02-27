package com.api_caich.service;

import java.io.StringReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.api_caich.dto.pago.ErrorSincronizacionDTO;
import com.api_caich.dto.pago.PagoSincronizacionDTO;
import com.api_caich.dto.pago.SincronizacionPreviewDTO;
import com.api_caich.dto.pago.SincronizacionResultadoDTO;
import com.api_caich.entity.Configuracion;
import com.api_caich.entity.Ticket;
import com.api_caich.enums.MetodoPago;
import com.api_caich.exception.BusinessException;
import com.api_caich.repository.ConfiguracionRepository;
import com.api_caich.repository.TicketRepository;
import com.opencsv.CSVReader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleFormsSyncService {

    private final PagoService pagoService;
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
        try {
            log.info("Iniciando previsualización de sincronización desde Google Sheets...");

            String csvContent = descargarCSV();
            
            try (CSVReader reader = new CSVReader(new StringReader(csvContent))) {
                List<String[]> filas = reader.readAll();

                List<PagoSincronizacionDTO> pagosValidos = new ArrayList<>();
                List<PagoSincronizacionDTO> pagosConError = new ArrayList<>();
                int totalDuplicados = 0;

                for (int i = 1; i < filas.size(); i++) {
                    String[] fila = filas.get(i);

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
        try {
            log.info("Iniciando sincronización confirmada desde Google Sheets...");

            SincronizacionPreviewDTO preview = previsualizarSincronizacion();
            
            List<String> foliosSincronizados = new ArrayList<>();
            List<ErrorSincronizacionDTO> errores = new ArrayList<>();

            // Sincronizar solo los válidos
            for (PagoSincronizacionDTO pago : preview.getPagosValidos()) {
                try {
                    log.info("Sincronizando folio: {}", pago.getFolio());
                    
                    String notas = "Sincronizado vía Google Forms. Reportado por: " + pago.getNombrePaciente();
                    
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

    /**
     * Método legacy - mantiene compatibilidad con código existente
     * @deprecated Use previsualizarSincronizacion() y confirmarSincronizacion() en su lugar
     */
    @Deprecated
    public void sincronizarDesdeForms(Long adminId) {
        log.warn("Usando método legacy sincronizarDesdeForms - considere usar el nuevo flujo de preview/confirmación");
        confirmarSincronizacion(adminId);
    }

    /**
     * Valida un registro individual del CSV
     */
    private PagoSincronizacionDTO validarRegistro(String[] fila, int numeroFila) {
        String folio = fila[2].trim();
        String nombrePaciente = fila[3].trim();
        String montoRaw = fila[4].replaceAll("[^0-9.]", "");
        String metodoStr = fila[5].trim().toUpperCase();
        String referencia = fila[6].trim();
        String comprobanteUrlDrive = fila[8].trim();

        PagoSincronizacionDTO.PagoSincronizacionDTOBuilder builder = PagoSincronizacionDTO.builder()
                .numeroFila(numeroFila)
                .folio(folio)
                .nombrePaciente(nombrePaciente)
                .referencia(referencia)
                .metodoPago(metodoStr)
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
        BigDecimal monto;
        try {
            monto = new BigDecimal(montoRaw.isEmpty() ? "0" : montoRaw);
            builder.monto(monto);
            
            if (monto.compareTo(BigDecimal.ZERO) <= 0) {
                return builder
                        .esValido(false)
                        .mensajeError("El monto debe ser mayor a 0")
                        .build();
            }
        } catch (NumberFormatException e) {
            return builder
                    .esValido(false)
                    .mensajeError("Formato de monto inválido: " + fila[4])
                    .build();
        }

        // Validación 4: Verificar duplicados
        boolean pagoYaExiste = ticket.getPagos() != null && 
                ticket.getPagos().stream()
                .anyMatch(p -> p.getMonto().compareTo(monto) == 0 
                        && p.getReferencia() != null 
                        && p.getReferencia().equals(referencia));
        
        if (pagoYaExiste) {
            return builder
                    .esValido(false)
                    .esDuplicado(true)
                    .mensajeError("Pago duplicado - ya existe un pago con el mismo monto y referencia")
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
        String comprobanteUrlLimpia = limpiarUrlDrive(comprobanteUrlDrive);
        if (comprobanteUrlLimpia == null || comprobanteUrlLimpia.isEmpty()) {
            return builder
                    .esValido(false)
                    .mensajeError("URL de comprobante inválida o vacía")
                    .build();
        }
        builder.comprobanteUrl(comprobanteUrlLimpia);

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
