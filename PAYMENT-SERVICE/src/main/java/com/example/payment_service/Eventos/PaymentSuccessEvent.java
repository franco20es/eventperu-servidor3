package com.example.payment_service.Eventos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSuccessEvent {
    private String transactionReference;
    private String culqiChargeId;
    private String userId;
    private String email;
    private BigDecimal amount;
    private String currency;
    private String paymentType;
    private LocalDateTime paidAt;
    private String eventType = "PAYMENT_SUCCESS";
}