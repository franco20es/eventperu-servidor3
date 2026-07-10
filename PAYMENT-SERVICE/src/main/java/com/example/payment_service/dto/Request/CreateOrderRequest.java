package com.example.payment_service.dto.Request;



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
public class CreateOrderRequest {

    private String userId;
    private String userEmail;
    private String userName;
    private String eventId;
    private String eventName;
    private String eventLocation;
    private LocalDateTime eventDate;
    private String ticketType;
    private Integer quantity;
    private BigDecimal unitPrice;
    private String transactionReference;
}