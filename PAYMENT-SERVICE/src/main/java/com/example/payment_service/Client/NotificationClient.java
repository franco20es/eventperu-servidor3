package com.example.payment_service.Client;

import com.example.payment_service.dto.Request.NotificationRequest;
import com.example.payment_service.dto.Request.TicketEmailRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "notification-service")
public interface NotificationClient {

    // Email con PDF adjunto (tickets y comprobantes)
    @PostMapping("/api/v1/notifications/ticket")
    void enviarTicketEmail(@RequestBody TicketEmailRequest request);

    // Notificación in-app + email simple
    @PostMapping("/api/v1/notifications")
    void crearNotificacion(@RequestBody NotificationRequest request);

}