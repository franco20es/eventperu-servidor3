package com.example.payment_service.dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagoKpisResponse {
    private String totalRecaudado;      // "S/ 12,500.00"
    private double totalRecaudadoNum;   // 12500.0
    private long   pagosExitosos;
    private long   pagosFallidos;
    private long   reembolsos;
    private String comisiones;          // "S/ 1,250.00"
    private double comisionesNum;
}
