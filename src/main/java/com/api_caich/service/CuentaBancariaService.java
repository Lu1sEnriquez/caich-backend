package com.api_caich.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api_caich.dto.cuenta.ActualizarCuentaBancariaDTO;
import com.api_caich.dto.cuenta.CrearCuentaBancariaDTO;
import com.api_caich.dto.cuenta.CuentaBancariaDTO;
import com.api_caich.entity.CuentaBancaria;
import com.api_caich.exception.BusinessException;
import com.api_caich.exception.ResourceNotFoundException;
import com.api_caich.repository.CuentaBancariaRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CuentaBancariaService {

    private final CuentaBancariaRepository cuentaBancariaRepository;
    private final AuditoriaService auditoriaService;

    public List<CuentaBancariaDTO> obtenerTodasCuentas() {
        List<CuentaBancaria> cuentas = cuentaBancariaRepository.findAll();
        return cuentas.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<CuentaBancariaDTO> buscarCuentas(String search) {
        // Para mantener la búsqueda sin paginación, usaremos un Pageable sin límites
        List<CuentaBancaria> cuentas = cuentaBancariaRepository
                .searchCuentas(search, org.springframework.data.domain.Pageable.unpaged())
                .getContent();
        return cuentas.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<CuentaBancariaDTO> obtenerCuentasPorEstado(Boolean activa) {
        List<CuentaBancaria> cuentas = cuentaBancariaRepository.findByEstaActiva(activa);
        return cuentas.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<CuentaBancariaDTO> obtenerCuentasPorBanco(String banco) {
        List<CuentaBancaria> cuentas = cuentaBancariaRepository.findByBancoContainingIgnoreCase(banco);
        return cuentas.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<CuentaBancariaDTO> obtenerCuentasPorTitular(String titular) {
        List<CuentaBancaria> cuentas = cuentaBancariaRepository.findByTitularContainingIgnoreCase(titular);
        return cuentas.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<CuentaBancariaDTO> obtenerCuentasActivas() {
        List<CuentaBancaria> cuentas = cuentaBancariaRepository.findByEstaActivaTrue();
        return cuentas.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public CuentaBancariaDTO obtenerCuentaPorId(Long cuentaId) {
        CuentaBancaria cuenta = cuentaBancariaRepository.findById(cuentaId)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta bancaria no encontrada con ID: " + cuentaId));
        return mapToDTO(cuenta);
    }

    @Transactional
    public CuentaBancariaDTO crearCuenta(CrearCuentaBancariaDTO crearCuentaDTO, Long usuarioId) {
        // Validar número de cuenta único
        if (cuentaBancariaRepository.existsByNumeroCuenta(crearCuentaDTO.getNumeroCuenta())) {
            throw new BusinessException("Ya existe una cuenta con el número: " + crearCuentaDTO.getNumeroCuenta());
        }

        // Crear cuenta
        CuentaBancaria cuenta = new CuentaBancaria();
        cuenta.setBanco(crearCuentaDTO.getBanco());
        cuenta.setNumeroCuenta(crearCuentaDTO.getNumeroCuenta());
        cuenta.setClabe(crearCuentaDTO.getClabe());
        cuenta.setTitular(crearCuentaDTO.getTitular());
        cuenta.setEstaActiva(crearCuentaDTO.getEstaActiva());

        CuentaBancaria savedCuenta = cuentaBancariaRepository.save(cuenta);

        log.info("Cuenta bancaria creada: {} - {}", savedCuenta.getBanco(), savedCuenta.getNumeroCuenta());

        // Auditoría
        auditoriaService.registrarCambio("CuentasBancarias", savedCuenta.getCuentaId(), "Crear",
                "Todos los campos", null, null, usuarioId, "SYSTEM");

        return mapToDTO(savedCuenta);
    }

    @Transactional
    public CuentaBancariaDTO actualizarCuenta(Long cuentaId, ActualizarCuentaBancariaDTO actualizarCuentaDTO,
            Long usuarioId) {
        CuentaBancaria cuenta = cuentaBancariaRepository.findById(cuentaId)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta bancaria no encontrada con ID: " + cuentaId));

        String cambios = "";

        // Validar y actualizar número de cuenta si es diferente
        if (actualizarCuentaDTO.getNumeroCuenta() != null
                && !actualizarCuentaDTO.getNumeroCuenta().equals(cuenta.getNumeroCuenta())) {

            if (cuentaBancariaRepository.existsByNumeroCuentaAndCuentaIdNot(actualizarCuentaDTO.getNumeroCuenta(),
                    cuentaId)) {
                throw new BusinessException(
                        "Ya existe una cuenta con el número: " + actualizarCuentaDTO.getNumeroCuenta());
            }
            cambios += "NumeroCuenta: " + cuenta.getNumeroCuenta() + " -> " + actualizarCuentaDTO.getNumeroCuenta()
                    + "; ";
            cuenta.setNumeroCuenta(actualizarCuentaDTO.getNumeroCuenta());
        }

        // Actualizar otros campos
        if (actualizarCuentaDTO.getBanco() != null) {
            cambios += "Banco: " + cuenta.getBanco() + " -> " + actualizarCuentaDTO.getBanco() + "; ";
            cuenta.setBanco(actualizarCuentaDTO.getBanco());
        }

        if (actualizarCuentaDTO.getClabe() != null) {
            cambios += "CLABE: " + cuenta.getClabe() + " -> " + actualizarCuentaDTO.getClabe() + "; ";
            cuenta.setClabe(actualizarCuentaDTO.getClabe());
        }

        if (actualizarCuentaDTO.getTitular() != null) {
            cambios += "Titular: " + cuenta.getTitular() + " -> " + actualizarCuentaDTO.getTitular() + "; ";
            cuenta.setTitular(actualizarCuentaDTO.getTitular());
        }

        if (actualizarCuentaDTO.getEstaActiva() != null) {
            cambios += "EstaActiva: " + cuenta.getEstaActiva() + " -> " + actualizarCuentaDTO.getEstaActiva() + "; ";
            cuenta.setEstaActiva(actualizarCuentaDTO.getEstaActiva());
        }

        CuentaBancaria updatedCuenta = cuentaBancariaRepository.save(cuenta);

        log.info("Cuenta bancaria actualizada: {} - Cambios: {}", updatedCuenta.getNumeroCuenta(), cambios);

        // Auditoría si hubo cambios
        if (!cambios.isEmpty()) {
            auditoriaService.registrarCambio("CuentasBancarias", cuentaId, "Actualizar",
                    cambios, null, null, usuarioId, "SYSTEM");
        }

        return mapToDTO(updatedCuenta);
    }

    @Transactional
    public CuentaBancariaDTO cambiarEstadoCuenta(Long cuentaId, Boolean estaActiva, Long usuarioId) {
        CuentaBancaria cuenta = cuentaBancariaRepository.findById(cuentaId)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta bancaria no encontrada con ID: " + cuentaId));

        Boolean estadoAnterior = cuenta.getEstaActiva();
        cuenta.setEstaActiva(estaActiva);

        CuentaBancaria updatedCuenta = cuentaBancariaRepository.save(cuenta);

        log.info("Estado de cuenta cambiado: {} - {} -> {}",
                cuenta.getNumeroCuenta(), estadoAnterior, estaActiva);

        // Auditoría
        String cambios = "EstaActiva: " + estadoAnterior + " -> " + estaActiva;
        auditoriaService.registrarCambio("CuentasBancarias", cuentaId, "Actualizar",
                cambios, null, null, usuarioId, "SYSTEM");

        return mapToDTO(updatedCuenta);
    }

    @Transactional
    public void eliminarCuenta(Long cuentaId, Long usuarioId) {
        CuentaBancaria cuenta = cuentaBancariaRepository.findById(cuentaId)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta bancaria no encontrada con ID: " + cuentaId));

        // Verificar si la cuenta tiene tickets relacionados
        // if (!cuenta.getTickets().isEmpty()) {
        //     throw new BusinessException("No se puede eliminar la cuenta porque tiene tickets relacionados");
        // }
        cuentaBancariaRepository.delete(cuenta);

        log.info("Cuenta bancaria eliminada: {} - {}", cuenta.getBanco(), cuenta.getNumeroCuenta());

        // Auditoría
        auditoriaService.registrarCambio("CuentasBancarias", cuentaId, "Eliminar",
                "Cuenta eliminada", null, null, usuarioId, "SYSTEM");
    }

    public Long contarCuentasActivas() {
        return cuentaBancariaRepository.countByEstaActivaTrue();
    }

    // Mapper
    private CuentaBancariaDTO mapToDTO(CuentaBancaria cuenta) {
        return CuentaBancariaDTO.builder()
                .cuentaId(cuenta.getCuentaId())
                .banco(cuenta.getBanco())
                .numeroCuenta(cuenta.getNumeroCuenta())
                .clabe(cuenta.getClabe())
                .titular(cuenta.getTitular())
                .estaActiva(cuenta.getEstaActiva())
                .build();
    }
}
