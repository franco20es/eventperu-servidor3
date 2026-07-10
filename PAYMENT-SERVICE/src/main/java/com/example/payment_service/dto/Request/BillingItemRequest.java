package com.example.payment_service.dto.Request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BillingItemRequest {
    private int cantidad;
    private String descripcion;
    private String zona;
    private double precioUnitario;
}
