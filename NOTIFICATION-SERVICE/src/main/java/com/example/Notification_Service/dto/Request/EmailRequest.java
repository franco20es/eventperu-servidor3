package com.example.Notification_Service.dto.Request;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailRequest {

    private String to;
    private String subject;
    private String body;
    private String eventoNombre;
    private String eventoFecha;
    private String eventoLugar;
}