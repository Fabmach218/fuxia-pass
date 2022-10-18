package com.app.pasarela.model.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ModelTarjetaEdit {
    private Integer id;
    private boolean active;
    private Double limDiario;
}
