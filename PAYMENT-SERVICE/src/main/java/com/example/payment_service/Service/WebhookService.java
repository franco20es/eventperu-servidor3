package com.example.payment_service.Service;

import com.example.payment_service.Client.EventServiceClient;
import com.example.payment_service.Client.NotificationClient;
import com.example.payment_service.Client.UserServiceClient;
import com.example.payment_service.Model.PaymentStatusModel;
import com.example.payment_service.Model.PaymentTransactionModel;
import com.example.payment_service.Repository.PaymentTransactionRepository;
import com.example.payment_service.Service.Sunat.BillingService;
import com.example.payment_service.dto.Request.BillingItemRequest;
import com.example.payment_service.dto.Request.BillingRequest;
import com.example.payment_service.dto.Request.CreateOrderRequest;
import com.example.payment_service.dto.Request.NotificationRequest;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {

    private final PaymentTransactionRepository transactionRepository;
    private final OrderService                 orderService;
    private final BillingService               billingService;
    private final NotificationClient           notificationClient;
    private final EventServiceClient           eventServiceClient;
    private final UserServiceClient userServiceClient;

    // ── Punto de entrada ──────────────────────────────────────────────
    @Transactional
    public void procesarWebhook(String type, String dataId) {
        if (!"payment".equals(type) || dataId == null) return;

        try {
            Payment mpPayment  = new PaymentClient().get(Long.parseLong(dataId));
            String externalRef = mpPayment.getExternalReference();
            String mpStatus    = mpPayment.getStatus();

            log.info("Pago MP id={} status={} ref={}", dataId, mpStatus, externalRef);

            PaymentTransactionModel tx = transactionRepository
                    .findByTransactionReference(externalRef).orElse(null);

            if (tx == null) {
                log.warn("Transacción no encontrada: {}", externalRef);
                return;
            }

            switch (mpStatus) {
                case "approved" -> procesarAprobado(tx, mpPayment);
                case "rejected" -> procesarRechazado(tx);
                case "pending"  -> procesarPendiente(tx);
                default         -> log.warn("Estado MP desconocido: {}", mpStatus);
            }

        } catch (MPException | MPApiException e) {
            log.error("Error consultando pago MP id={}: {}", dataId, e.getMessage());
        }
    }

    // ── Pago aprobado ─────────────────────────────────────────────────
    private void procesarAprobado(PaymentTransactionModel tx, Payment mpPayment) {
        if (tx.getStatus() == PaymentStatusModel.COMPLETED) return;

        tx.setStatus(PaymentStatusModel.COMPLETED);
        tx.setGatewayTransactionId(mpPayment.getId().toString());
        tx.setUpdatedAt(LocalDateTime.now());
        transactionRepository.save(tx);

        // Si es membresía → activar plan, no crear tickets
        if (tx.getEventId() != null && tx.getEventId().startsWith("MEMBERSHIP-")) {
            String plan = tx.getEventId().replace("MEMBERSHIP-", "");
            try {
                userServiceClient.activarMembresia(tx.getUserId(), plan, tx.getTransactionReference());
                log.info("Membresía {} activada para userId={}", plan, tx.getUserId());
            } catch (Exception e) {
                log.error("Error activando membresía: {}", e.getMessage());
            }
            notificar(tx, "MEMBRESIA", "Membresía activada",
                    String.format("Tu plan %s fue activado. Disfruta tus beneficios.", plan));
            return;
        }

        // Pago normal → reducir capacidad + crear tickets
        try {
            if (tx.getZonaId() != null && !tx.getZonaId().isBlank()) {
                eventServiceClient.reducirDisponibles(tx.getEventId(), tx.getZonaId(), tx.getQuantity());
            }
            eventServiceClient.reducirCapacidad(tx.getEventId(), tx.getQuantity());
            log.info("Capacidad reducida evento={} cantidad={}", tx.getEventId(), tx.getQuantity());
        } catch (Exception e) {
            log.error("Error reduciendo capacidad: {}", e.getMessage());
        }

        crearOrdenYTickets(tx);
        emitirComprobante(tx);
        notificar(tx, "PAGO", "Pago confirmado",
                String.format("Tu pago de S/ %.2f para %s fue aprobado.",
                        tx.getAmount(), tx.getEventName()));
    }

    // ══════════════════════════════════════════════════════════════════════
// TODO-DEPLOY: Este método confirmarManual() es PROVISIONAL para desarrollo
// En producción con dominio real, MP llama al webhook automáticamente.
// Al desplegar: ELIMINAR este método y el endpoint /confirm del controller.
// ══════════════════════════════════════════════════════════════════════
    @Transactional
    public void confirmarManual(String reference) {
        PaymentTransactionModel tx = transactionRepository
                .findByTransactionReference(reference)
                .orElseThrow(() -> new RuntimeException("Transacción no encontrada: " + reference));

        if (tx.getStatus() == PaymentStatusModel.COMPLETED) {
            log.info("Transacción {} ya estaba confirmada", reference);
            return;
        }

        tx.setStatus(PaymentStatusModel.COMPLETED);
        tx.setUpdatedAt(LocalDateTime.now());
        transactionRepository.save(tx);

        // Si es membresía → activar plan
        if (tx.getEventId() != null && tx.getEventId().startsWith("MEMBERSHIP-")) {
            String plan = tx.getEventId().replace("MEMBERSHIP-", "");
            try {
                userServiceClient.activarMembresia(tx.getUserId(), plan, tx.getTransactionReference());
                log.info("[DEV] Membresía {} activada para userId={}", plan, tx.getUserId());
            } catch (Exception e) {
                log.error("[DEV] Error activando membresía: {}", e.getMessage());
            }
            notificar(tx, "MEMBRESIA", "Membresía activada",
                    String.format("Tu plan %s fue activado.", plan));
            log.info("[DEV] Membresía confirmada manualmente ref={}", reference);
            return;
        }

        // Pago normal → reducir capacidad + tickets
        try {
            if (tx.getZonaId() != null && !tx.getZonaId().isBlank()) {
                eventServiceClient.reducirDisponibles(tx.getEventId(), tx.getZonaId(), tx.getQuantity());
                log.info("[DEV] Zona reducida OK zonaId={}", tx.getZonaId());
            }
            eventServiceClient.reducirCapacidad(tx.getEventId(), tx.getQuantity());
            log.info("[DEV] Capacidad reducida OK evento={} cantidad={}", tx.getEventId(), tx.getQuantity());
        } catch (Exception e) {
            log.error("[DEV] Error reduciendo capacidad: {}", e.getMessage(), e);
        }

        crearOrdenYTickets(tx);
        emitirComprobante(tx);
        notificar(tx, "PAGO", "Pago confirmado",
                String.format("Tu pago de S/ %.2f para %s fue aprobado.",
                        tx.getAmount(), tx.getEventName()));
        log.info("[DEV] Pago confirmado manualmente ref={}", reference);
    }


    // ── Pago rechazado ────────────────────────────────────────────────
    private void procesarRechazado(PaymentTransactionModel tx) {
        tx.setStatus(PaymentStatusModel.FAILED);
        tx.setUpdatedAt(LocalDateTime.now());
        transactionRepository.save(tx);

        notificar(tx, "PAGO",
                " Pago rechazado",
                String.format("Tu pago para %s fue rechazado. Intenta con otro método de pago.",
                        tx.getEventName()));

        log.warn("Pago rechazado ref={}", tx.getTransactionReference());
    }

    // ── Pago pendiente ────────────────────────────────────────────────
    private void procesarPendiente(PaymentTransactionModel tx) {
        notificar(tx, "PAGO",
                "⏳ Pago en proceso",
                String.format("Tu pago para %s está siendo procesado. Te notificaremos cuando se confirme.",
                        tx.getEventName()));

        log.info("Pago pendiente ref={}", tx.getTransactionReference());
    }

    // ── Crear orden y tickets ─────────────────────────────────────────
    private void crearOrdenYTickets(PaymentTransactionModel tx) {
        try {
            double precioUnitario = tx.getAmount() / tx.getQuantity();

            CreateOrderRequest request = CreateOrderRequest.builder()
                    .userId(tx.getUserId())
                    .userEmail(tx.getUserEmail())
                    .userName(tx.getUserName())
                    .eventId(tx.getEventId())
                    .eventName(tx.getEventName())
                    .eventLocation(tx.getEventLocation())
                    .eventDate(tx.getEventDate())
                    .quantity(tx.getQuantity())
                    .unitPrice(BigDecimal.valueOf(precioUnitario))
                    .transactionReference(tx.getTransactionReference())
                    .ticketType(tx.getTicketType())
                    .build();

            orderService.createOrder(request);
            log.info("Orden y tickets generados para {}", tx.getTransactionReference());

        } catch (Exception e) {
            log.error("Error generando orden para {}: {}",
                    tx.getTransactionReference(), e.getMessage());
        }
    }

    // ── Emitir comprobante SUNAT ──────────────────────────────────────
    private void emitirComprobante(PaymentTransactionModel tx) {
        try {
            String tipoComprobante = tx.getTipoComprobante() != null
                    ? tx.getTipoComprobante() : "boleta";
            String clienteDocumento = tx.getClienteDocumento() != null
                    ? tx.getClienteDocumento() : "00000000";
            String clienteTipoDoc = tx.getClienteTipoDoc() != null
                    ? tx.getClienteTipoDoc() : "1";

            BillingRequest billing = BillingRequest.builder()
                    .tipoComprobante(tipoComprobante)
                    .transactionReference(tx.getTransactionReference())
                    .usuarioId(tx.getUserId())
                    .clienteNombre(tx.getUserName() != null ? tx.getUserName() : tx.getUserEmail())
                    .clienteDocumento(clienteDocumento)
                    .clienteTipoDoc(clienteTipoDoc)
                    .clienteEmail(tx.getUserEmail())
                    .razonSocial(tx.getRazonSocial())
                    .eventoNombre(tx.getEventName())
                    .eventoFecha(tx.getEventDate() != null
                            ? tx.getEventDate().toLocalDate().toString() : "")
                    .eventoLugar(tx.getEventLocation())
                    .items(List.of(
                            BillingItemRequest.builder()
                                    .descripcion(tx.getTicketType() + " — " + tx.getEventName())
                                    .zona(tx.getTicketType())
                                    .cantidad(tx.getQuantity())
                                    .precioUnitario(tx.getAmount())
                                    .build()
                    ))
                    .build();

            billingService.emitirComprobante(billing);
            log.info("Comprobante {} emitido para {}", tipoComprobante, tx.getTransactionReference());

        } catch (Exception e) {
            log.warn("No se pudo emitir comprobante para {}: {}",
                    tx.getTransactionReference(), e.getMessage());
        }
    }

    // ── Enviar notificación in-app ────────────────────────────────────
    private void notificar(PaymentTransactionModel tx, String tipo,
                           String titulo, String mensaje) {
        try {
            notificationClient.crearNotificacion(
                    NotificationRequest.builder()
                            .usuarioId(tx.getUserId())
                            .titulo(titulo)
                            .mensaje(mensaje)
                            .tipo(tipo)
                            .eventoId(tx.getEventId())
                            .eventoNombre(tx.getEventName())
                            .emailDestino(tx.getUserEmail())
                            .build()
            );
            log.info("Notificación '{}' enviada a usuario {}", titulo, tx.getUserId());
        } catch (Exception e) {
            log.warn("No se pudo enviar notificación: {}", e.getMessage());
        }
    }
}