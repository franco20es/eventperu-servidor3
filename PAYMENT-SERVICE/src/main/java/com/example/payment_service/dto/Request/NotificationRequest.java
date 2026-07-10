package com.example.payment_service.dto.Request;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {
    private String usuarioId;
    private String titulo;
    private String mensaje;
    private String tipo;          // PAGO, REEMBOLSO, TICKET, EVENTO, SISTEMA
    private String eventoId;
    private String eventoNombre;
    private String emailDestino;
    private String telefonoDestino;
}
