package com.app.pasarela.model.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ModelTransferencia {
    private String nroTarjetaOrigen;
    private String dueMonthOrigen;
    private String dueYearOrigen;
    private String cvvOrigen;
    private String nombreOrigen;
    private String monedaOrigen;
    private Double montoOrigen;
    private String nroTarjetaDestino;
    private String dueMonthDestino;
    private String dueYearDestino;
    private String cvvDestino;
    private String nombreDestino;
    private String monedaDestino;
    private Double montoDestino;
    private String descripcion;
}
