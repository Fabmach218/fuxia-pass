package com.app.pasarela.model;

import lombok.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "t_tarjeta")
public class Tarjeta {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(columnDefinition = "char(19)", unique = true, updatable = false)
    @NotNull
    private String nroTarjeta;

    @Column(columnDefinition = "char(7)", updatable = false)
    @NotNull
    private String dueDate;

    @Column(columnDefinition = "char(3)", updatable = false)
    @NotNull
    private String cvv;

    @Column(updatable = false)
    @NotNull
    private String nombre;

    @Column(columnDefinition = "char(1)", updatable = false)
    @NotNull
    private String tipo;

    @NotNull
    private boolean active;

    @Column(columnDefinition = "char(8)", updatable = false)
    @NotNull
    private String dni;

    @Column(columnDefinition = "char(3)", updatable = false)
    @NotNull
    private String moneda;

    @Column(columnDefinition = "numeric(18,2)")
    @NotNull
    private Double saldo;

}
