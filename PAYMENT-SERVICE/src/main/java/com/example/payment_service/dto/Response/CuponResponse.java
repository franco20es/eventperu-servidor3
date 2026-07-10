package com.example.payment_service.dto.Response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CuponResponse {
    private String       id;
    private String       codigo;
    private String       descripcion;
    private String       tipoDescuento;
    private BigDecimal   valorDescuento;
    private BigDecimal   montoMinimo;
    private BigDecimal   descuentoMaximo;
    private String       eventoId;
    private Integer      limiteUsos;
    private Integer      usosActuales;
    private Integer      usosDisponibles;
    private double       pctUso;           // porcentaje de uso
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaExpiracion;
    private String       estado;
    private String       creadoPor;
    private LocalDateTime createdAt;
}
