package com.example.payment_service.dto.Response;


import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TicketAdminResponse {
    private String       id;
    private String       code;
    private String       userId;
    private String       userName;
    private String       userEmail;
    private String       eventId;
    private String       eventName;
    private String       eventLocation;
    private LocalDateTime eventDate;
    private String       ticketType;
    private BigDecimal   price;
    private String       status;       // ACTIVE | USED | CANCELLED | PENDING | REFUNDED
    private Boolean      used;
    private LocalDateTime usedAt;
    private LocalDateTime createdAt;
    // ── campos calculados para el frontend ──
    private String       precioFormateado;  // "S/ 150.00"
    private String       estadoLabel;       // "Válido" | "Usado" | "Cancelado"
    private String       fechaEventoFmt;    // "14 Jun 2026"
    private String       fechaCompraFmt;    // "14 Jun 2026 01:30"
}