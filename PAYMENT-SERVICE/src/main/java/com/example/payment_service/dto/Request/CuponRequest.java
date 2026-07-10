package com.example.payment_service.dto.Request;

import com.example.payment_service.Model.TipoDescuento;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CuponRequest {

    @NotBlank(message = "El código es obligatorio")
    @Size(min = 3, max = 50, message = "El código debe tener entre 3 y 50 caracteres")
    @Pattern(regexp = "^[A-Z0-9_-]+$", message = "El código solo puede contener letras mayúsculas, números, guiones y guiones bajos")
    private String codigo;

    @NotBlank(message = "La descripción es obligatoria")
    private String descripcion;

    @NotNull(message = "El tipo de descuento es obligatorio")
    private TipoDescuento tipoDescuento;

    @NotNull(message = "El valor del descuento es obligatorio")
    @DecimalMin(value = "0.01", message = "El descuento debe ser mayor a 0")
    private BigDecimal valorDescuento;

    private BigDecimal montoMinimo;      // opcional

    private BigDecimal descuentoMaximo;  // opcional, solo para PORCENTAJE

    private String eventoId;             // null = todos los eventos

    @NotNull(message = "El límite de usos es obligatorio")
    @Min(value = 1, message = "El límite de usos debe ser al menos 1")
    private Integer limiteUsos;

    @NotNull(message = "La fecha de inicio es obligatoria")
    private LocalDateTime fechaInicio;

    @NotNull(message = "La fecha de expiración es obligatoria")
    @Future(message = "La fecha de expiración debe ser futura")
    private LocalDateTime fechaExpiracion;
}