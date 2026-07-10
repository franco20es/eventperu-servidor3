package com.example.payment_service.dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngresoMesResponse {
    private String mes;    // "Ene", "Feb", ...
    private double valor;
}