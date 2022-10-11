package com.app.pasarela.model.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ModelRespuestaTarjeta {
    private String status;
    private ModelTarjetaCreate tarjeta;
    private String mensaje;
}
