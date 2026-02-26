package com.api_caich.service;

import java.io.StringReader;
import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.api_caich.entity.Configuracion;
import com.api_caich.entity.Ticket;
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

    public void sincronizarDesdeForms(Long adminId) {
        try {
            log.info("Iniciando descarga de CSV desde Google Sheets...");

            Configuracion config = configRepository.findByClave("GOOGLE_FORMS_CSV_URL")
                    .orElseThrow(() -> new BusinessException("No se encontró la URL de sincronización en la BD"));

            String csvUrl = config.getValor();

            String csvContent = restTemplate.getForObject(csvUrl, String.class);
            if (csvContent == null || csvContent.isEmpty()) {
                throw new BusinessException("El archivo CSV de Google está vacío o no es accesible.");
            }

            try (CSVReader reader = new CSVReader(new StringReader(csvContent))) {
                List<String[]> filas = reader.readAll();

                for (int i = 1; i < filas.size(); i++) {
                    String[] fila = filas.get(i);

                    if (fila.length < 9) continue;

                    String folio = fila[2].trim();
                    String nombrePaciente = fila[3];
                    String montoRaw = fila[4].replaceAll("[^0-9.]", "");
                    BigDecimal monto = new BigDecimal(montoRaw.isEmpty() ? "0" : montoRaw);
                    String metodoStr = fila[5].toUpperCase();
                    String referencia = fila[6];
                    String notas = "Sincronizado vía Google Forms. Reportado por: " + nombrePaciente;
                    String comprobanteUrlDrive = fila[8];

                    log.debug("Procesando folio: {}", folio);

                    Ticket ticket = ticketRepository.findByFolio(folio)
                            .orElseThrow(() -> new BusinessException("No se encontró el Ticket con folio: " + folio));

                    boolean pagoYaExiste = ticket.getPagos() != null && 
                            ticket.getPagos().stream()
                            .anyMatch(p -> p.getMonto().compareTo(monto) == 0);
                    
                    if (pagoYaExiste) {
                        log.info("Pago duplicado detectado para folio {}. Saltando...", folio);
                        continue;
                    }

                    String comprobanteUrlLimpia = limpiarUrlDrive(comprobanteUrlDrive);
                    log.info("URL de Drive para folio {}: {}", folio, comprobanteUrlLimpia);

                    pagoService.registrarPagoConComprobante(
                            ticket.getTicketId(),
                            monto,
                            metodoStr,
                            referencia,
                            notas,
                            comprobanteUrlLimpia,
                            adminId
                    );
                }
            }
            log.info("Sincronización de Google Forms completada con éxito.");

        } catch (Exception e) {
            log.error("Error crítico durante la sincronización: ", e);
            throw new RuntimeException("Fallo al procesar el registro de pagos: " + e.getMessage());
        }
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
