package com.example.payment_service.Service;

import com.example.payment_service.Client.NotificationClient;
import com.example.payment_service.Model.OrderModel;
import com.example.payment_service.Model.OrderStatus;
import com.example.payment_service.Model.PaymentStatusModel;
import com.example.payment_service.Model.PaymentTransactionModel;
import com.example.payment_service.Model.TicketStatus;
import com.example.payment_service.Repository.OrderRepository;
import com.example.payment_service.Repository.PaymentTransactionRepository;
import com.example.payment_service.dto.Request.NotificationRequest;
import com.example.payment_service.dto.Request.RefundRequest;
import com.example.payment_service.dto.Response.RefundResponse;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentRefundClient;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.PaymentRefund;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefundService {

    @Value("${mercadopago.access-token}")
    private String accessToken;

    @Value("${mercadopago.sandbox:true}")
    private boolean sandbox;

    private final PaymentTransactionRepository transactionRepository;
    private final OrderRepository              orderRepository;
    private final NotificationClient           notificationClient;

    @Transactional
    public RefundResponse procesarReembolso(RefundRequest request) {

        // 1. Buscar orden
        OrderModel orden = orderRepository
                .findByOrderReference(request.getOrderReference())
                .orElseThrow(() -> new RuntimeException(
                        "Orden no encontrada: " + request.getOrderReference()));

        // 2. Validar estado
        if (orden.getStatus() == OrderStatus.CANCELLED ||
                orden.getStatus() == OrderStatus.REFUNDED) {
            throw new RuntimeException("La orden ya fue cancelada o reembolsada");
        }

        // 3. Buscar transacción
        PaymentTransactionModel transaction = transactionRepository
                .findByTransactionReference(orden.getTransactionReference())
                .orElseThrow(() -> new RuntimeException(
                        "Transacción no encontrada para la orden"));

        String mpPaymentId = transaction.getGatewayTransactionId();
        if (mpPaymentId == null || mpPaymentId.isBlank()) {
            throw new RuntimeException("No se encontró el ID de pago de Mercado Pago");
        }

        try {
            PaymentRefund refund = null;

            if (sandbox) {
                log.info("Reembolso SIMULADO (sandbox) para mpPaymentId={}", mpPaymentId);
            } else {
                MercadoPagoConfig.setAccessToken(accessToken);
                PaymentRefundClient refundClient = new PaymentRefundClient();
                refund = refundClient.refund(Long.parseLong(mpPaymentId));
                log.info("Reembolso MP creado: id={} status={}", refund.getId(), refund.getStatus());
            }

            // 4. Actualizar transacción
            transaction.setStatus(PaymentStatusModel.REFUNDED);
            transaction.setUpdatedAt(LocalDateTime.now());
            transactionRepository.save(transaction);

            // 5. Cancelar orden y tickets
            orden.setStatus(OrderStatus.REFUNDED);
            if (orden.getTickets() != null) {
                orden.getTickets().forEach(t -> t.setStatus(TicketStatus.CANCELLED));
            }
            orderRepository.save(orden);
            log.info("Orden {} reembolsada correctamente", orden.getOrderReference());

            // 6. Notificar al usuario
            try {
                notificationClient.crearNotificacion(
                        NotificationRequest.builder()
                                .usuarioId(transaction.getUserId())
                                .titulo("💸 Reembolso procesado")
                                .mensaje(String.format(
                                        "Tu reembolso de S/ %.2f por %s fue procesado. " +
                                                "El dinero se acreditará en 3-5 días hábiles.",
                                        transaction.getAmount(), transaction.getEventName()))
                                .tipo("REEMBOLSO")
                                .eventoId(transaction.getEventId())
                                .eventoNombre(transaction.getEventName())
                                .emailDestino(transaction.getUserEmail())
                                .build()
                );
            } catch (Exception e) {
                log.warn("No se pudo enviar notificación de reembolso: {}", e.getMessage());
            }

            return RefundResponse.builder()
                    .success(true)
                    .orderReference(orden.getOrderReference())
                    .mpRefundId(sandbox ? "SANDBOX-" + mpPaymentId : refund.getId().toString())
                    .monto(transaction.getAmount())
                    .motivo(request.getMotivo())
                    .mensaje("Reembolso procesado. El dinero se acreditará en 3-5 días hábiles.")
                    .build();

        } catch (MPApiException e) {
            log.error("MP API Error status={} response={}",
                    e.getStatusCode(), e.getApiResponse().getContent());
            throw new RuntimeException("Error MP: " + e.getApiResponse().getContent());
        } catch (MPException e) {
            log.error("MP Exception: {}", e.getMessage());
            throw new RuntimeException("Error MP: " + e.getMessage());
        }
    }
}