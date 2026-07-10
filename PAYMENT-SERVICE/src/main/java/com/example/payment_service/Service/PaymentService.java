package com.example.payment_service.Service;

import com.example.payment_service.Model.PaymentTransactionModel;
import com.example.payment_service.dto.Request.PaymentRequestDTO;
import com.example.payment_service.dto.Response.PaymentResponseDTO;

public interface PaymentService {
    PaymentResponseDTO createPayment(PaymentRequestDTO request);

    PaymentTransactionModel getTransactionByReference(String transactionReference);
}
