package com.example.payment_service.dto.Request;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TicketEmailRequest {
    private String emailDestino;
    private String usuarioId;
    private String nombreUsuario;
    private String eventoNombre;
    private String eventoFecha;
    private String eventoLugar;
    private String ticketCode;
    private String orderReference;
    private byte[] pdfTicket;
}
