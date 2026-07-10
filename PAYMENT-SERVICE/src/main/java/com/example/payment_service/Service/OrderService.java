package com.example.payment_service.Service;


import com.example.payment_service.Model.OrderModel;
import com.example.payment_service.dto.Request.CreateOrderRequest;

import java.util.List;

public interface OrderService {

    OrderModel createOrder(CreateOrderRequest request);

    OrderModel getByOrderReference(String orderReference);

    OrderModel getByTransactionReference(String transactionReference);

    List<OrderModel> getOrdersByUser(String userId);

    void cancelOrder(String orderReference);
}