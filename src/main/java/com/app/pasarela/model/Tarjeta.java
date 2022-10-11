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

    @Column(columnDefinition = "char(19)", unique = true)
    @NotNull
    private String nroTarjeta;

    @Column(columnDefinition = "char(7)")
    @NotNull
    private String dueDate;

    @Column(columnDefinition = "char(3)")
    @NotNull
    private String cvv;

    @NotNull
    private String nombre;

    @Column(columnDefinition = "char(1)")
    @NotNull
    private String tipo;

    @NotNull
    private boolean active;

    @Column(columnDefinition = "char(8)")
    @NotNull
    private String dni;

    @Column(columnDefinition = "char(3)")
    @NotNull
    private String moneda;

    @Column(columnDefinition = "numeric(18,2)")
    @NotNull
    private Double saldo;

}
