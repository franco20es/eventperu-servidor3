package com.example.payment_service.dto.Response;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidarCuponResponse {
    private boolean    valido;
    private String     codigo;
    private String     mensaje;
    private String     tipoDescuento;
    private BigDecimal valorDescuento;
    private BigDecimal montoCompra;
    private BigDecimal montoDescuento;
    private BigDecimal montoFinal;
}
