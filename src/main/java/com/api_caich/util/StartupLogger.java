package com.api_caich.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Slf4j
@Component
public class StartupLogger {

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${server.servlet.context-path:/}")
    private String contextPath;

    @Value("${app.name:CAICH API}")
    private String appName;

    @Value("${app.version:1.0.0}")
    private String appVersion;

    @EventListener(ApplicationReadyEvent.class)
    public void logApplicationStartup() {
        try {
            String hostAddress = InetAddress.getLocalHost().getHostAddress();
            String swaggerUrl = String.format("http://%s:%s%s/swagger-ui.html", hostAddress, serverPort, contextPath);
            String apiDocsUrl = String.format("http://%s:%s%s/v3/api-docs", hostAddress, serverPort, contextPath);
            String healthUrl = String.format("http://%s:%s%s/health", hostAddress, serverPort, contextPath);

            log.info("╔══════════════════════════════════════════════════════════════════════════════╗");
            log.info("║                            {} v{}                           ║", appName, appVersion);
            log.info("╠══════════════════════════════════════════════════════════════════════════════╣");
            log.info("║ Application has started successfully!                                        ║");
            log.info("║                                                                              ║");
            log.info("║ Local:          http://localhost:{}{}                              ║", serverPort, contextPath);
            log.info("║ Network:        http://{}:{}{}                            ║", hostAddress, serverPort,
                    contextPath);
            log.info("║                                                                              ║");
            log.info("║ Swagger UI:     {} ║", swaggerUrl);
            log.info("║ API Docs:       {}       ║", apiDocsUrl);
            log.info("║ Health Check:   {}         ║", healthUrl);
            log.info("║                                                                              ║");
            log.info("║ Context Path:   {}                                                  ║", contextPath);
            log.info("║ Port:           {}                                                          ║", serverPort);
            log.info("║ Environment:    {}                                                ║",
                    System.getProperty("spring.profiles.active", "default"));
            log.info("╚══════════════════════════════════════════════════════════════════════════════╝");

        } catch (UnknownHostException e) {
            log.error("Could not determine host address", e);
        }
    }
}
