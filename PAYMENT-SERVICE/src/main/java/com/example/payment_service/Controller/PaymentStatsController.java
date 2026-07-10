package com.example.payment_service.Controller;

import com.example.payment_service.Service.PaymentStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments/admin")
@RequiredArgsConstructor
public class PaymentStatsController {

    private final PaymentStatsService statsService;

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(statsService.getStats());
    }
}