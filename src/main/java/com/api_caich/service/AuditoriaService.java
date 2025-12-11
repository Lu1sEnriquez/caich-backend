package com.api_caich.service;

import com.api_caich.dto.auditoria.HistorialCambioDTO;
import com.api_caich.entity.HistorialCambios;
import com.api_caich.entity.Usuario;
import com.api_caich.repository.HistorialCambiosRepository;
import com.api_caich.repository.UsuarioRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private final HistorialCambiosRepository historialRepository;
    private final UsuarioRepository usuarioRepository;
    private final ObjectMapper objectMapper;

    public void registrarCambio(String entidad, Long entidadId, String accion,
            String camposModificados, String valoresAnteriores,
            String valoresNuevos, Long usuarioId, String direccionIP) {

        HistorialCambios historial = new HistorialCambios();
        historial.setEntidad(entidad);
        historial.setEntidadId(entidadId);
        historial.setAccion(accion);
        historial.setCamposModificados(camposModificados);
        historial.setValoresAnteriores(valoresAnteriores);
        historial.setValoresNuevos(valoresNuevos);
        historial.setFechaCambio(LocalDateTime.now());
        historial.setDireccionIP(direccionIP);

        if (usuarioId != null) {
            Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
            historial.setUsuario(usuario);
        }

        historialRepository.save(historial);
        log.debug("Audit log created for {} {}: {}", entidad, entidadId, accion);
    }

    public <T> void registrarCambioConObjetos(String entidad, Long entidadId, String accion,
            T objetoAnterior, T objetoNuevo,
            Long usuarioId, String direccionIP) {

        try {
            String valoresAnteriores = objetoAnterior != null ? objectMapper.writeValueAsString(objetoAnterior) : null;
            String valoresNuevos = objetoNuevo != null ? objectMapper.writeValueAsString(objetoNuevo) : null;

            registrarCambio(entidad, entidadId, accion, "Todos los campos",
                    valoresAnteriores, valoresNuevos, usuarioId, direccionIP);

        } catch (JsonProcessingException e) {
            log.error("Error serializing objects for audit log", e);
            registrarCambio(entidad, entidadId, accion, "Error serializando objetos",
                    null, null, usuarioId, direccionIP);
        }
    }

    public List<HistorialCambioDTO> obtenerHistorial(String entidad, Long entidadId) {
        List<HistorialCambios> historial = historialRepository
                .findByEntidadAndEntidadIdOrderByFechaCambioDesc(entidad, entidadId);

        return historial.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<HistorialCambioDTO> obtenerHistorialPorUsuario(Long usuarioId, int limite) {
        List<HistorialCambios> historial = historialRepository
                .findByUsuarioUsuarioIdOrderByFechaCambioDesc(usuarioId);

        if (limite > 0) {
            historial = historial.stream().limit(limite).collect(Collectors.toList());
        }

        return historial.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Scheduled(cron = "0 0 3 1 * *")
    @Transactional
    public void limpiarHistorialAntiguo() {
        LocalDateTime fechaLimite = LocalDateTime.now().minusYears(1);
        int eliminados = historialRepository.deleteByFechaCambioBefore(fechaLimite);

        log.info("Cleaned up {} old audit records", eliminados);
    }

    private HistorialCambioDTO mapToDTO(HistorialCambios historial) {
        return HistorialCambioDTO.builder()
                .cambioId(historial.getCambioId())
                .entidad(historial.getEntidad())
                .entidadId(historial.getEntidadId())
                .accion(historial.getAccion())
                .camposModificados(historial.getCamposModificados())
                .valoresAnteriores(historial.getValoresAnteriores())
                .valoresNuevos(historial.getValoresNuevos())
                .usuarioNombre(historial.getUsuario() != null ? historial.getUsuario().getNombreCompleto() : "Sistema")
                .fechaCambio(historial.getFechaCambio())
                .direccionIP(historial.getDireccionIP())
                .build();
    }
}