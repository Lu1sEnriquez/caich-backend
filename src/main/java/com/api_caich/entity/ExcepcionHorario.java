package com.api_caich.entity;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad que define excepciones al HorarioBase.
 * Se usa para bloqueos, días festivos, mantenimientos, cambios puntuales, etc.
 * 
 * Si horaInicioBloqueo y horaFinBloqueo son NULL y todoElDia=true, significa cierre total.
 * Si todoElDia=false, solo se bloquean los horarios especificados.
 * 
 * Ejemplo:
 * - 25/12/2025, todoElDia=true → Cerrado todo el día (Navidad)
 * - 15/02/2026, todoElDia=false, 08:00-12:00 → Mantenimiento solo en la mañana
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "ExcepcionHorario", indexes = {
    @Index(name = "IDX_excepcion_fecha", columnList = "Fecha"),
    @Index(name = "IDX_excepcion_espacio_fecha", columnList = "EspacioId, Fecha")
})
public class ExcepcionHorario extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ExcepcionHorarioId")
    private Long excepcionHorarioId;

    /**
     * Espacio asociado. Si es NULL, es una excepción global.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "EspacioId")
    private Espacio espacio;

    /**
     * Fecha de la excepción.
     */
    @Column(name = "Fecha", nullable = false)
    private LocalDate fecha;

    /**
     * Indica si el bloqueo es para todo el día.
     * Si es true, horaInicioBloqueo y horaFinBloqueo pueden ser NULL.
     */
    @Builder.Default
    @Column(name = "TodoElDia", nullable = false)
    private Boolean todoElDia = false;

    /**
     * Hora de inicio del bloqueo.
     * Si NULL y todoElDia=true, la excepción cubre todo el día.
     * Si NULL y todoElDia=false, el espacio abre normalmente pero con restricciones en horaFinBloqueo.
     */
    @Column(name = "HoraInicioBloqueo")
    private LocalTime horaInicioBloqueo;

    /**
     * Hora de fin del bloqueo.
     * Si NULL y todoElDia=true, la excepción cubre todo el día.
     * Si NULL y todoElDia=false, el espacio cierra desde horaInicioBloqueo.
     */
    @Column(name = "HoraFinBloqueo")
    private LocalTime horaFinBloqueo;

    /**
     * Motivo del bloqueo o excepción (ej: "Navidad", "Mantenimiento", "Reunión importante").
     */
    @Column(name = "Motivo", length = 255)
    private String motivo;

    /**
     * Indica si esta excepción está activa.
     */
    @Builder.Default
    @Column(name = "EstaActivo", nullable = false)
    private Boolean estaActivo = true;

    /**
     * Verifica si un horario está bloqueado dentro de esta excepción.
     * @param horaInicio Hora de inicio a verificar
     * @param horaFin Hora de fin a verificar
     * @return true si el horario está dentro del bloqueo
     */
    public boolean estaBloqueado(LocalTime horaInicio, LocalTime horaFin) {
        if (!estaActivo) {
            return false;
        }

        // Si es todo el día, todo está bloqueado
        if (todoElDia) {
            return true;
        }

        // Si no es todo el día, verificar si hay solapamiento
        if (horaInicioBloqueo == null || horaFinBloqueo == null) {
            return false;
        }

        // Solapamiento: inicio < fin_bloqueo Y fin > inicio_bloqueo
        return horaInicio.isBefore(horaFinBloqueo) && horaFin.isAfter(horaInicioBloqueo);
    }
}
