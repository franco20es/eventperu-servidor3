package com.example.Notification_Service.Controller;

import com.example.Notification_Service.Service.NotificationService;
import com.example.Notification_Service.dto.Request.NotificationRequest;
import com.example.Notification_Service.dto.Request.TicketEmailRequest;
import com.example.Notification_Service.dto.Response.NotificationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    // ═══ INTERNO: otros microservicios lo llaman vía Feign ═══
    @PostMapping
    public ResponseEntity<NotificationResponse> crear(
            @Valid @RequestBody NotificationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(notificationService.crearNotificacion(request));
    }

    // ═══ USUARIO: mis notificaciones ═══
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<Page<NotificationResponse>> porUsuario(
            @PathVariable String usuarioId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                notificationService.obtenerPorUsuario(usuarioId, PageRequest.of(page, size))
        );
    }

    @GetMapping("/usuario/{usuarioId}/no-leidas")
    public ResponseEntity<List<NotificationResponse>> noLeidas(
            @PathVariable String usuarioId) {
        return ResponseEntity.ok(notificationService.obtenerNoLeidas(usuarioId));
    }

    @GetMapping("/usuario/{usuarioId}/count")
    public ResponseEntity<Map<String, Long>> contarNoLeidas(
            @PathVariable String usuarioId) {
        return ResponseEntity.ok(
                Map.of("noLeidas", notificationService.contarNoLeidas(usuarioId))
        );
    }

    @PutMapping("/{id}/leer")
    public ResponseEntity<Void> marcarLeida(@PathVariable String id) {
        notificationService.marcarComoLeida(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/usuario/{usuarioId}/leer-todas")
    public ResponseEntity<Void> marcarTodasLeidas(@PathVariable String usuarioId) {
        notificationService.marcarTodasLeidas(usuarioId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        notificationService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/ticket")
    public ResponseEntity<Void> enviarTicketPorEmail(
            @RequestBody TicketEmailRequest request) {
        log.info("Enviando ticket por email a: {}", request.getEmailDestino());
        notificationService.enviarTicketEmail(request);
        return ResponseEntity.ok().build();
    }
}
