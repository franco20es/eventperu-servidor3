package com.example.Notification_Service.dto.Request;


import lombok.Data;

@Data
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