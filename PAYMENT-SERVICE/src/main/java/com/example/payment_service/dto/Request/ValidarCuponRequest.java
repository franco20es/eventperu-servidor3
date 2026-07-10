package com.example.payment_service.dto.Request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ValidarCuponRequest {

    @NotBlank(message = "El código del cupón es obligatorio")
    private String codigo;

    @NotNull(message = "El monto de la compra es obligatorio")
    private BigDecimal montoCompra;

    private String eventoId; // para validar si el cupón aplica al evento
}