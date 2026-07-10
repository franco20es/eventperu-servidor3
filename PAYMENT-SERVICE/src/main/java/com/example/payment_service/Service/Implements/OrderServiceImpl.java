package com.example.payment_service.Service.Implements;

import com.example.payment_service.Client.NotificationClient;
import com.example.payment_service.Model.*;
import com.example.payment_service.Repository.OrderRepository;
import com.example.payment_service.Repository.TicketRepository;
import com.example.payment_service.Service.OrderService;
import com.example.payment_service.Util.PdfTicketUtil;
import com.example.payment_service.dto.Request.CreateOrderRequest;
import com.example.payment_service.dto.Request.TicketEmailRequest;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final TicketRepository ticketRepository;
    private final NotificationClient notificationClient;
    private final PdfTicketUtil pdfTicketUtil;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    @Transactional
    public OrderModel createOrder(CreateOrderRequest request) {
        log.info("Creando orden para usuario: {} evento: {}",
                request.getUserId(), request.getEventId());

        String orderReference = "ORD-" + UUID.randomUUID()
                .toString().substring(0, 8).toUpperCase();

//        LocalDateTime eventDate = request.getEventDate() != null
//                ? request.getEventDate()
//                : LocalDateTime.now().plusDays(30);  // fecha futura por defecto
//

        OrderModel order = OrderModel.builder()
                .orderReference(orderReference)
                .userId(request.getUserId())
                .userEmail(request.getUserEmail())
                .userName(request.getUserName())
                .eventId(request.getEventId())
                .eventName(request.getEventName())
                .eventLocation(request.getEventLocation())
                .eventDate(request.getEventDate() != null ? request.getEventDate() : LocalDateTime.now().plusDays(30))
                .quantity(request.getQuantity())
//                .eventDate(eventDate)
                .unitPrice(request.getUnitPrice())
                .totalAmount(request.getUnitPrice()
                        .multiply(java.math.BigDecimal.valueOf(request.getQuantity())))
                .transactionReference(request.getTransactionReference())
                .ticketType(request.getTicketType())
                .status(OrderStatus.PAID)
                .build();

        OrderModel savedOrder = orderRepository.save(order);
        log.info("Orden creada: {}", orderReference);

        List<TicketModel> tickets = new ArrayList<>();
        for (int i = 0; i < request.getQuantity(); i++) {
            String ticketCode = generateTicketCode();
            String qrToken = generateQrToken(ticketCode, request);

            TicketModel ticket = TicketModel.builder()
                    .code(ticketCode)
                    .qrToken(qrToken)
                    .order(savedOrder)
                    .userId(request.getUserId())
                    .userEmail(request.getUserEmail())
                    .userName(request.getUserName())
                    .eventId(request.getEventId())
                    .eventName(request.getEventName())
                    .eventDate(request.getEventDate() != null ? request.getEventDate() : LocalDateTime.now().plusDays(30))
                    .eventLocation(request.getEventLocation())
                    .ticketType(request.getTicketType())
                    .price(request.getUnitPrice())
                    .status(TicketStatus.ACTIVE)
                    .used(false)
                    .build();

            TicketModel savedTicket = ticketRepository.save(ticket);
            tickets.add(savedTicket);
            log.info("Ticket generado: {}", ticketCode);

            // ← Generar PDF y enviar email
            try {
                byte[] pdf = pdfTicketUtil.generateTicketPdf(savedTicket);

                TicketEmailRequest emailRequest = TicketEmailRequest.builder()
                        .emailDestino(request.getUserEmail())
                        .usuarioId(request.getUserId())
                        .nombreUsuario(request.getUserName())
                        .eventoNombre(request.getEventName())
                        .eventoFecha(request.getEventDate() != null
                                ? request.getEventDate().toString() : "")
                        .eventoLugar(request.getEventLocation())
                        .ticketCode(ticketCode)
                        .orderReference(orderReference)
                        .pdfTicket(pdf)
                        .build();

                notificationClient.enviarTicketEmail(emailRequest);
                log.info("Email de ticket enviado a: {}", request.getUserEmail());

            } catch (Exception e) {
                log.warn("No se pudo enviar email del ticket {}: {}", ticketCode, e.getMessage());
            }
        }

        savedOrder.setTickets(tickets);
        log.info("Orden {} con {} tickets generados", orderReference, tickets.size());
        return savedOrder;
    }

    @Override
    public OrderModel getByOrderReference(String orderReference) {
        return orderRepository.findByOrderReference(orderReference)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada: " + orderReference));
    }

    @Override
    public OrderModel getByTransactionReference(String transactionReference) {
        return orderRepository.findByTransactionReference(transactionReference)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada para transacción: "
                        + transactionReference));
    }

    @Override
    public List<OrderModel> getOrdersByUser(String userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    @Transactional
    public void cancelOrder(String orderReference) {
        OrderModel order = getByOrderReference(orderReference);
        order.setStatus(OrderStatus.CANCELLED);
        order.getTickets().forEach(t -> t.setStatus(TicketStatus.CANCELLED));
        orderRepository.save(order);
        log.info("Orden cancelada: {}", orderReference);
    }

    private String generateTicketCode() {
        return "EVT-" + UUID.randomUUID().toString()
                .replace("-", "").substring(0, 12).toUpperCase();
    }

    //agregado para el angualar
    private String generateQrToken(String ticketCode, CreateOrderRequest request) {


        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 64) {
            byte[] padded = new byte[64];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            keyBytes = padded;
        }
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);

        String jwt = Jwts.builder()
                .subject(ticketCode)
                .claim("eventId", request.getEventId())
                .claim("userId", request.getUserId())
                .claim("ticketType", request.getTicketType())
                .issuedAt(new Date())
                .signWith(key)
                .compact();

        // ← QR apunta al frontend Angular
        return "https://eventperu.francowe.me/ticket/verify?token=" + jwt;
    }
}