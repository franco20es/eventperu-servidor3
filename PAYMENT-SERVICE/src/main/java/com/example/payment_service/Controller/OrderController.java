package com.example.payment_service.Controller;


import com.example.payment_service.Model.OrderModel;
import com.example.payment_service.Service.OrderService;
import com.example.payment_service.Service.RefundService;
import com.example.payment_service.dto.Request.RefundRequest;
import com.example.payment_service.dto.Response.RefundResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;
    private final RefundService refundService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderModel>> getOrdersByUser(@PathVariable String userId) {
        log.info("Obteniendo órdenes de usuario: {}", userId);
        return ResponseEntity.ok(orderService.getOrdersByUser(userId));
    }

    @GetMapping("/{orderReference}")
    public ResponseEntity<OrderModel> getOrder(@PathVariable String orderReference) {
        log.info("Obteniendo orden: {}", orderReference);
        return ResponseEntity.ok(orderService.getByOrderReference(orderReference));
    }

    @DeleteMapping("/{orderReference}/cancel")
    public ResponseEntity<Void> cancelOrder(@PathVariable String orderReference) {
        log.info("Cancelando orden: {}", orderReference);
        orderService.cancelOrder(orderReference);
        return ResponseEntity.noContent().build();
    }

    // ── Reembolso via Mercado Pago ─────────────────────────────────────
    @PostMapping("/{orderReference}/refund")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_ADMIN')")
    public ResponseEntity<RefundResponse> solicitarReembolso(
            @PathVariable String orderReference,
            @RequestBody RefundRequest request) {
        log.info("Solicitando reembolso para orden: {}", orderReference);
        request.setOrderReference(orderReference);
        RefundResponse response = refundService.procesarReembolso(request);
        return ResponseEntity.ok(response);
    }
}