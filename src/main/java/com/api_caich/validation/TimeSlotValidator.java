package com.api_caich.validation;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.api_caich.dto.horario.ValidacionReservaResponseDTO;
import com.api_caich.entity.HorarioBase;
import com.api_caich.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Componente para validar que las reservas cumplan con la cuadrícula de tiempo configurada.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TimeSlotValidator {

    /**
     * Valida que una reserva cumpla con todas las reglas del horario base.
     * 
     * @param horaInicio Hora de inicio de la reserva
     * @param horaFin Hora de fin de la reserva
     * @param horarioBase Horario base del espacio
     * @return DTO con resultado de validación
     */
    public ValidacionReservaResponseDTO validarReserva(
            LocalTime horaInicio, 
            LocalTime horaFin, 
            HorarioBase horarioBase) {
        
        List<ValidacionReservaResponseDTO.ErrorValidacionDTO> errores = new ArrayList<>();

        // 1. Validar que las horas estén dentro del horario operativo
        validarHorarioOperativo(horaInicio, horaFin, horarioBase, errores);

        // 2. Validar alineación con la cuadrícula
        validarAlineacionConCuadricula(horaInicio, horaFin, horarioBase, errores);

        // 3. Validar duración mínima
        validarDuracionMinima(horaInicio, horaFin, horarioBase, errores);

        // 4. Validar duración máxima
        validarDuracionMaxima(horaInicio, horaFin, horarioBase, errores);

        boolean esValido = errores.isEmpty();
        long duracionMinutos = Duration.between(horaInicio, horaFin).toMinutes();
        int slotsOcupados = (int) (duracionMinutos / horarioBase.getIntervaloMinutos());

        return ValidacionReservaResponseDTO.builder()
                .esValido(esValido)
                .duracionMinutos((int) duracionMinutos)
                .slotsOcupados(slotsOcupados)
                .errores(esValido ? null : errores)
                .build();
    }

    /**
     * Valida y lanza excepción si la reserva no es válida.
     */
    public void validarYLanzarExcepcion(
            LocalTime horaInicio, 
            LocalTime horaFin, 
            HorarioBase horarioBase) {
        
        ValidacionReservaResponseDTO resultado = validarReserva(horaInicio, horaFin, horarioBase);
        
        if (!resultado.getEsValido()) {
            String mensajesError = resultado.getErrores().stream()
                    .map(ValidacionReservaResponseDTO.ErrorValidacionDTO::getMensaje)
                    .reduce((a, b) -> a + ". " + b)
                    .orElse("Error de validación en horario");
            
            throw new BusinessException(mensajesError);
        }
    }

    private void validarHorarioOperativo(
            LocalTime horaInicio, 
            LocalTime horaFin, 
            HorarioBase horarioBase,
            List<ValidacionReservaResponseDTO.ErrorValidacionDTO> errores) {
        
        if (horaInicio.isBefore(horarioBase.getHoraApertura())) {
            errores.add(ValidacionReservaResponseDTO.ErrorValidacionDTO.builder()
                    .codigo("HORA_ANTES_APERTURA")
                    .campo("horaInicio")
                    .mensaje("La hora de inicio no puede ser antes de la hora de apertura (" + 
                            horarioBase.getHoraApertura() + ")")
                    .valorRecibido(horaInicio.toString())
                    .build());
        }

        if (horaFin.isAfter(horarioBase.getHoraCierre())) {
            errores.add(ValidacionReservaResponseDTO.ErrorValidacionDTO.builder()
                    .codigo("HORA_DESPUES_CIERRE")
                    .campo("horaFin")
                    .mensaje("La hora de fin no puede ser después de la hora de cierre (" + 
                            horarioBase.getHoraCierre() + ")")
                    .valorRecibido(horaFin.toString())
                    .build());
        }
    }

    private void validarAlineacionConCuadricula(
            LocalTime horaInicio, 
            LocalTime horaFin, 
            HorarioBase horarioBase,
            List<ValidacionReservaResponseDTO.ErrorValidacionDTO> errores) {
        
        if (!horarioBase.esHoraAlineada(horaInicio)) {
            List<String> horasValidas = calcularHorasValidasCercanas(horaInicio, horarioBase);
            errores.add(ValidacionReservaResponseDTO.ErrorValidacionDTO.builder()
                    .codigo("HORA_INICIO_NO_ALINEADA")
                    .campo("horaInicio")
                    .mensaje("La hora de inicio no está alineada con la cuadrícula de " + 
                            horarioBase.getIntervaloMinutos() + " minutos")
                    .valorRecibido(horaInicio.toString())
                    .valoresValidos(horasValidas)
                    .build());
        }

        if (!horarioBase.esHoraAlineada(horaFin)) {
            List<String> horasValidas = calcularHorasValidasCercanas(horaFin, horarioBase);
            errores.add(ValidacionReservaResponseDTO.ErrorValidacionDTO.builder()
                    .codigo("HORA_FIN_NO_ALINEADA")
                    .campo("horaFin")
                    .mensaje("La hora de fin no está alineada con la cuadrícula de " + 
                            horarioBase.getIntervaloMinutos() + " minutos")
                    .valorRecibido(horaFin.toString())
                    .valoresValidos(horasValidas)
                    .build());
        }
    }

    private void validarDuracionMinima(
            LocalTime horaInicio, 
            LocalTime horaFin, 
            HorarioBase horarioBase,
            List<ValidacionReservaResponseDTO.ErrorValidacionDTO> errores) {
        
        long duracionMinutos = Duration.between(horaInicio, horaFin).toMinutes();
        
        if (duracionMinutos < horarioBase.getDuracionMinimaMinutos()) {
            errores.add(ValidacionReservaResponseDTO.ErrorValidacionDTO.builder()
                    .codigo("DURACION_MENOR_MINIMA")
                    .campo("duracion")
                    .mensaje("La duración de la reserva (" + duracionMinutos + " min) es menor a la mínima permitida (" + 
                            horarioBase.getDuracionMinimaMinutos() + " min)")
                    .valorRecibido(duracionMinutos + " minutos")
                    .build());
        }
    }

    private void validarDuracionMaxima(
            LocalTime horaInicio, 
            LocalTime horaFin, 
            HorarioBase horarioBase,
            List<ValidacionReservaResponseDTO.ErrorValidacionDTO> errores) {
        
        long duracionMinutos = Duration.between(horaInicio, horaFin).toMinutes();
        
        if (duracionMinutos > horarioBase.getDuracionMaximaMinutos()) {
            errores.add(ValidacionReservaResponseDTO.ErrorValidacionDTO.builder()
                    .codigo("DURACION_MAYOR_MAXIMA")
                    .campo("duracion")
                    .mensaje("La duración de la reserva (" + duracionMinutos + " min) es mayor a la máxima permitida (" + 
                            horarioBase.getDuracionMaximaMinutos() + " min)")
                    .valorRecibido(duracionMinutos + " minutos")
                    .build());
        }
    }

    /**
     * Calcula las 2 horas válidas más cercanas a una hora dada.
     */
    private List<String> calcularHorasValidasCercanas(LocalTime hora, HorarioBase horarioBase) {
        List<String> horasValidas = new ArrayList<>();
        
        LocalTime anterior = encontrarHoraAlineadaAnterior(hora, horarioBase);
        LocalTime siguiente = encontrarHoraAlineadaSiguiente(hora, horarioBase);
        
        if (anterior != null) {
            horasValidas.add(anterior.toString());
        }
        if (siguiente != null) {
            horasValidas.add(siguiente.toString());
        }
        
        return horasValidas;
    }

    private LocalTime encontrarHoraAlineadaAnterior(LocalTime hora, HorarioBase horarioBase) {
        long minutosDesdeApertura = Duration.between(horarioBase.getHoraApertura(), hora).toMinutes();
        if (minutosDesdeApertura < 0) {
            return null;
        }
        
        long slotsDesdeApertura = minutosDesdeApertura / horarioBase.getIntervaloMinutos();
        return horarioBase.getHoraApertura().plusMinutes(slotsDesdeApertura * horarioBase.getIntervaloMinutos());
    }

    private LocalTime encontrarHoraAlineadaSiguiente(LocalTime hora, HorarioBase horarioBase) {
        LocalTime anterior = encontrarHoraAlineadaAnterior(hora, horarioBase);
        if (anterior == null) {
            return horarioBase.getHoraApertura();
        }
        
        LocalTime siguiente = anterior.plusMinutes(horarioBase.getIntervaloMinutos());
        if (siguiente.isAfter(horarioBase.getHoraCierre())) {
            return null;
        }
        
        return siguiente;
    }
}
