package com.example.payment_service.dto.Response;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TicketKpisResponse {
    private long   total;
    private long   activos;
    private long   usados;
    private long   cancelados;
    private long   pendientes;
    private String recaudado;       // "S/ 12,500.00"
    private double recaudadoNum;
}
