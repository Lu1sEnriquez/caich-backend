package com.api_caich.entity;

import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad que define el horario base recurrente de un espacio.
 * Dictamina el horario normal de funcionamiento.
 * 
 * Ejemplo: Lunes a Viernes 07:00-18:00, Sábado 07:00-14:00, Domingo cerrado
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "HorarioBase", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"EspacioId"})
})
public class HorarioBase extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "HorarioBaseId")
    private Long horarioBaseId;

    /**
     * Espacio asociado. Si es NULL, esta es la configuración global por defecto.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "EspacioId")
    private Espacio espacio;

    /**
     * Hora de apertura del espacio (ej: 07:00).
     */
    @Column(name = "HoraApertura", nullable = false)
    private LocalTime horaApertura;

    /**
     * Hora de cierre del espacio (ej: 18:00).
     */
    @Column(name = "HoraCierre", nullable = false)
    private LocalTime horaCierre;

    /**
     * Días operativos de la semana.
     * Formato: "1,2,3,4,5" donde 1=Lunes, 2=Martes, ..., 7=Domingo
     * NULL o vacío = Todos los días
     * Ejemplo: "1,2,3,4,5" = Lunes a Viernes
     *          "1,2,3,4,5,6" = Lunes a Sábado
     */
    @Column(name = "DiasOperativos", length = 50)
    private String diasOperativos;

    /**
     * Intervalo de tiempo en minutos para la cuadrícula de slots (ej: 30, 60, 90).
     * Define la "granularidad" con la que se pueden hacer reservas.
     */
    @Column(name = "IntervaloMinutos", nullable = false)
    private Integer intervaloMinutos;

    /**
     * Duración mínima permitida para una reserva en minutos (ej: 30).
     */
    @Column(name = "DuracionMinimaMinutos", nullable = false)
    private Integer duracionMinimaMinutos;

    /**
     * Duración máxima permitida para una reserva en minutos (ej: 240 = 4 horas).
     */
    @Column(name = "DuracionMaximaMinutos", nullable = false)
    private Integer duracionMaximaMinutos;

    /**
     * Indica si esta configuración está activa.
     */
    @Builder.Default
    @Column(name = "EstaActivo", nullable = false)
    private Boolean estaActivo = true;

    /**
     * Verifica si un día de la semana está operativo.
     * @param dayOfWeek Day of week (1=Monday, 7=Sunday)
     * @return true si el día está operativo
     */
    public boolean isDiaOperativo(int dayOfWeek) {
        if (diasOperativos == null || diasOperativos.trim().isEmpty()) {
            return true; // Si no hay restricción, todos los días son operativos
        }
        
        String[] dias = diasOperativos.split(",");
        for (String dia : dias) {
            try {
                if (Integer.parseInt(dia.trim()) == dayOfWeek) {
                    return true;
                }
            } catch (NumberFormatException e) {
                // Ignorar formatos inválidos
            }
        }
        return false;
    }

    /**
     * Calcula el número de slots entre dos horas.
     */
    public int calcularNumeroSlots(LocalTime inicio, LocalTime fin) {
        long minutos = java.time.Duration.between(inicio, fin).toMinutes();
        return (int) (minutos / intervaloMinutos);
    }

    /**
     * Verifica si una hora está alineada con la cuadrícula de intervalos.
     */
    public boolean esHoraAlineada(LocalTime hora) {
        long minutosDesdeApertura = java.time.Duration.between(horaApertura, hora).toMinutes();
        return minutosDesdeApertura >= 0 && minutosDesdeApertura % intervaloMinutos == 0;
    }
}
