package com.example.payment_service.dto.Response;

import lombok.*;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ReporteEventoResponse {
    private String      eventoId;
    private String      eventoNombre;
    private long        totalTickets;
    private long        ticketsVendidos;
    private long        ticketsUsados;
    private long        ticketsCancelados;
    private String      recaudado;
    private double      recaudadoNum;
    private double      pctOcupacion;
    private List<ZonaReporteResponse> zonas;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ZonaReporteResponse {
        private String nombre;
        private String color;
        private long   total;
        private long   vendidos;
        private long   usados;
        private long   cancelados;
        private long   disponibles;
        private double pct;
        private String recaudado;
        private double recaudadoNum;
    }
}