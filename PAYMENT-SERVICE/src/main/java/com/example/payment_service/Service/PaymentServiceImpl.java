package com.example.payment_service.Service;

import com.example.payment_service.Model.PaymentStatusModel;
import com.example.payment_service.Model.PaymentTransactionModel;
import com.example.payment_service.Repository.PaymentTransactionRepository;
import com.example.payment_service.dto.Request.PaymentRequestDTO;
import com.example.payment_service.dto.Response.PaymentResponseDTO;
import com.mercadopago.resources.preference.Preference;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final MercadoPagoService mercadoPagoService;
    private final PaymentTransactionRepository transactionRepository;

    @Value("${app.mp-sandbox:true}")
    private boolean mpSandbox;

    @Override
    @Transactional
    public PaymentResponseDTO createPayment(PaymentRequestDTO request) {

        String transactionReference = "TXN-" + UUID.randomUUID()
                .toString().substring(0, 8).toUpperCase();

        log.info("Iniciando pago MP ref={} user={} evento={} sandbox={}",
                transactionReference, request.getUserId(), request.getEvenNombre(), mpSandbox);

        // 1. Guardar transacción PENDING
        LocalDateTime eventDate = null;
        if (request.getEvenFecha() != null && !request.getEvenFecha().isBlank()) {
            try {
                eventDate = LocalDate.parse(request.getEvenFecha()).atStartOfDay();
            } catch (Exception e) {
                log.warn("No se pudo parsear evenFecha: {}", request.getEvenFecha());
            }
        }

        PaymentTransactionModel transaction = PaymentTransactionModel.builder()
                .transactionReference(transactionReference)
                .userId(request.getUserId())
                .userEmail(request.getEmail())
                .userName(request.getUserName())
                .eventId(request.getEventId())
                .zonaId(request.getZonaId())
                .eventName(request.getEvenNombre())
                .eventDate(eventDate)
                .eventLocation(request.getEvenLugar())
                .ticketType(request.getTicketType())
                .quantity(request.getQuantity())
                .amount(request.getUnitPrice()
                        .multiply(java.math.BigDecimal.valueOf(request.getQuantity()))
                        .doubleValue())
                .status(PaymentStatusModel.PENDING)
                .build();

        transactionRepository.save(transaction);

        // 2. Crear preference en Mercado Pago
        Preference preference = mercadoPagoService.crearPreferencia(request, transactionReference);

        // 3. Guardar preferenceId
        transaction.setGatewayTransactionId(preference.getId());
        transactionRepository.save(transaction);

        // 4. Resolver URL de checkout según entorno
        String checkoutUrl = mpSandbox
                ? preference.getSandboxInitPoint()
                : preference.getInitPoint();

        log.info("Preference creada id={} ref={} checkoutUrl={}",
                preference.getId(), transactionReference, checkoutUrl);

        return PaymentResponseDTO.builder()
                .checkoutUrl(checkoutUrl)
                .preferenceId(preference.getId())
                .transactionReference(transactionReference)
                .status("PENDING")
                .message("Redirige al usuario a checkoutUrl para completar el pago")
                .eventName(request.getEvenNombre())
                .quantity(request.getQuantity())
                .totalAmount(transaction.getAmount())
                .build();
    }

    @Override
    public PaymentTransactionModel getTransactionByReference(String transactionReference) {
        return transactionRepository.findByTransactionReference(transactionReference)
                .orElseThrow(() -> new RuntimeException(
                        "Transacción no encontrada: " + transactionReference));
    }
}