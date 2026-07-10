package com.example.payment_service.dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngresoEventoResponse {
    private String eventoId;
    private String nombre;
    private String color;
    private String monto;     // "S/ 5,000.00"
    private double montoNum;
}

