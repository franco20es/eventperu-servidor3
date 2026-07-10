package com.example.Notification_Service.dto.Response;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private String id;
    private String usuarioId;
    private String titulo;
    private String mensaje;
    private String tipo;
    private Boolean leido;
    private String eventoId;
    private String eventoNombre;
    private Boolean emailEnviado;
    private LocalDateTime createdAt;
}