package com.example.payment_service.Controller;

import com.example.payment_service.Repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class HealthController {

    private final PaymentTransactionRepository transactionRepository;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "payment-service");
        health.put("timestamp", LocalDateTime.now());
        health.put("hostname", System.getenv("HOSTNAME"));

        log.debug("Health check realizado");
        return ResponseEntity.ok(health);
    }

    @GetMapping("/ready")
    public ResponseEntity<Map<String, Object>> ready() {
        Map<String, Object> ready = new HashMap<>();
        ready.put("status", "READY");

        // Verificar base de datos
        try {
            long count = transactionRepository.count();
            ready.put("database", "UP");
            ready.put("transactions_count", count);
        } catch (Exception e) {
            log.error(" Error conectando a BD: {}", e.getMessage());
            ready.put("database", "DOWN");
            ready.put("database_error", e.getMessage());
            return ResponseEntity.status(503).body(ready);
        }

        log.debug("Ready check realizado");
        return ResponseEntity.ok(ready);
    }
}
