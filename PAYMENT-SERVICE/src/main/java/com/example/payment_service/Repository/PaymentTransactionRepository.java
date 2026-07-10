package com.example.payment_service.Repository;

import com.example.payment_service.Model.PaymentTransactionModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransactionModel, String> {

    Optional<PaymentTransactionModel> findByTransactionReference(String transactionReference);

    Optional<PaymentTransactionModel> findByGatewayTransactionId(String gatewayTransactionId);

    Page<PaymentTransactionModel> findByUserId(String userId, Pageable pageable);
}
