package com.example.payment_service.Model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransactionModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String transactionReference;
    private String gatewayTransactionId;

    // ── Usuario ────────────────────────────────────────────────────────
    private String userId;
    private String userEmail;
    private String userName;

    // ── Comprobante — guardados desde el PaymentRequest ───────────────
    private String tipoComprobante;   // "boleta" | "factura"
    private String clienteDocumento;  // DNI o RUC
    private String clienteTipoDoc;    // "1" = DNI, "6" = RUC
    private String razonSocial;       // solo para factura

    // ── Evento ─────────────────────────────────────────────────────────
    private String eventId;
    private String eventName;
    private String eventLocation;
    private LocalDateTime eventDate;
    private String ticketType;
    private String zonaId;

    // ── Monto ──────────────────────────────────────────────────────────
    private Double amount;
    private Integer quantity;

    // ── Estado ─────────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    private PaymentStatusModel status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}