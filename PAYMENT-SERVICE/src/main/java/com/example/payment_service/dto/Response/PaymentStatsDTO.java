package com.example.payment_service.dto.Response;

import com.example.payment_service.Model.PaymentTransactionModel;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PaymentStatsDTO {
    private String userId;
    private BigDecimal totalSpent;
    private long totalTransactions;
    private long successfulTransactions;
    private PaymentTransactionModel lastTransaction;
}