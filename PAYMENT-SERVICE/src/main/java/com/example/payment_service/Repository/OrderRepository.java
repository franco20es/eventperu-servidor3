package com.example.payment_service.Repository;


import com.example.payment_service.Model.OrderModel;
import com.example.payment_service.Model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<OrderModel, String> {

    Optional<OrderModel> findByOrderReference(String orderReference);

    Optional<OrderModel> findByTransactionReference(String transactionReference);

    List<OrderModel> findByUserId(String userId);

    List<OrderModel> findByUserIdOrderByCreatedAtDesc(String userId);

    List<OrderModel> findByEventId(String eventId);

    boolean existsByTransactionReference(String transactionReference);
}