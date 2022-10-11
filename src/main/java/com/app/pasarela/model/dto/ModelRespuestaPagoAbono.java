package com.app.pasarela.model.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ModelRespuestaPagoAbono {
    private String status;
    private ModelPagoAbono tarjeta;
    private String mensaje;
}
