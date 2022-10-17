package com.app.pasarela.model;

import java.io.Serializable;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "t_usuario")

public class Usuario implements Serializable{
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(columnDefinition = "char(8)", unique = true, updatable = false)
    @NotNull
    @NotBlank(message = "El DNI es obligatorio")
    private String username;

    @NotNull
    @NotBlank(message = "El nombre es obligatorio")
    private String nombres;
    
    @NotNull
    @NotBlank(message = "El nombre es obligatorio")
    private String apellidos;
    
    @Column(unique = true, updatable = false)
    @NotNull
    @NotBlank(message = "El email es obligatorio")
    private String email;
    
    @NotNull
    @NotBlank(message = "La contraseña es obligatoria")
    private String password;
    
    @NotNull
    @Size(max = 1)
    private String tipoUsuario = "C";

}
