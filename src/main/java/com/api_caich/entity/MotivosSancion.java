package com.api_caich.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "MotivosSancion")
public class MotivosSancion extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MotivoSancionId")
    private Long motivoSancionId;
    
    @Column(name = "Descripcion", nullable = false, length = 200)
    private String descripcion;

}
