package com.api_caich.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.api_caich.dto.sancion.MotivoSancionDeleteResponseDTO;
import com.api_caich.dto.sancion.MotivoSancionRequestDTO;
import com.api_caich.dto.sancion.MotivoSancionResponseDTO;
import com.api_caich.entity.MotivosSancion;
import com.api_caich.repository.MotivosSancionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MotivosSancionService {
    private final MotivosSancionRepository motivosSancionRepository;

    public List<MotivoSancionResponseDTO> getAllMotivosSancion() {
        return motivosSancionRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public MotivoSancionResponseDTO createMotivoSancion(MotivoSancionRequestDTO requestDTO) {
        MotivosSancion motivo = new MotivosSancion();
        motivo.setDescripcion(requestDTO.getDescripcion());
        return toResponseDTO(motivosSancionRepository.save(motivo));
    }

    public MotivoSancionResponseDTO updateMotivoSancion(Long id, MotivoSancionRequestDTO requestDTO) {
        MotivosSancion motivo = motivosSancionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Motivo de sanción no encontrado"));
        motivo.setDescripcion(requestDTO.getDescripcion());
        return toResponseDTO(motivosSancionRepository.save(motivo));
    }

    public MotivoSancionDeleteResponseDTO deleteMotivoSancion(Long id) {
        motivosSancionRepository.deleteById(id);
        return MotivoSancionDeleteResponseDTO.builder()
                .motivoSancionId(id)
                .eliminado(true)
                .build();
    }

    private MotivoSancionResponseDTO toResponseDTO(MotivosSancion motivo) {
        return MotivoSancionResponseDTO.builder()
                .motivoSancionId(motivo.getMotivoSancionId())
                .descripcion(motivo.getDescripcion())
                .fechaCreacion(motivo.getFechaCreacion())
                .ultimaActualizacion(motivo.getUltimaActualizacion())
                .build();
    }
}
