package com.example.payment_service.Controller;

import com.example.payment_service.Model.ComprobanteModel;
import com.example.payment_service.Model.PaymentStatusModel;
import com.example.payment_service.Model.PaymentTransactionModel;
import com.example.payment_service.Repository.ComprobanteRepository;

import com.example.payment_service.Service.PaymentServiceImpl;
import com.example.payment_service.Service.Sunat.BillingService;
import com.example.payment_service.Service.WebhookService;
import com.example.payment_service.dto.Request.BillingRequest;
import com.example.payment_service.dto.Request.PaymentRequestDTO;
import com.example.payment_service.dto.Response.BillingResponse;
import com.example.payment_service.dto.Response.PaymentResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@Slf4j
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentServiceImpl paymentService;
    private final BillingService billingService;
    private final ComprobanteRepository comprobanteRepository;
    private final WebhookService webhookService;

    @PostMapping("/create")
    public ResponseEntity<PaymentResponseDTO> createPayment(
            @Valid @RequestBody PaymentRequestDTO request) {

        // ← ya no usa request.getAmount() — ahora usa unitPrice * quantity
        log.info("Solicitud de pago: userId={} evento={}",
                request.getUserId(), request.getEvenNombre());

        PaymentResponseDTO response = paymentService.createPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{transactionReference:(?!admin).*}")
    public ResponseEntity<PaymentTransactionModel> getTransaction(
            @PathVariable String transactionReference) {
        return ResponseEntity.ok(
                paymentService.getTransactionByReference(transactionReference));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Payments Service - Online");
    }

    // ── Comprobantes SUNAT (sin cambios) ───────────────────────────────

    @PostMapping("/billing")
    public ResponseEntity<BillingResponse> emitirComprobante(
            @Valid @RequestBody BillingRequest request) {
        log.info("Emitiendo comprobante tipo={}", request.getTipoComprobante());
        return ResponseEntity.ok(billingService.emitirComprobante(request));
    }

    @GetMapping("/comprobantes/{usuarioId}")
    public ResponseEntity<List<ComprobanteModel>> obtenerComprobantes(
            @PathVariable String usuarioId) {
        return ResponseEntity.ok(comprobanteRepository
                .findByUsuarioIdOrderByFechaEmisionDesc(usuarioId));
    }

    @GetMapping("/comprobantes/numero/{numero}")
    public ResponseEntity<ComprobanteModel> obtenerComprobante(
            @PathVariable String numero) {
        return ResponseEntity.ok(comprobanteRepository
                .findByNumeroComprobante(numero)
                .orElseThrow(() -> new RuntimeException("Comprobante no encontrado")));
    }
    @GetMapping("/comprobantes/{id}/pdf")
    public ResponseEntity<byte[]> descargarPdf(@PathVariable String id) {
        ComprobanteModel comp = comprobanteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comprobante no encontrado"));

        byte[] pdf = Base64.getDecoder().decode(comp.getPdfBase64());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + comp.getNumeroComprobante() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(
            @RequestParam(value = "type",    required = false) String type,
            @RequestParam(value = "data.id", required = false) String dataId,
            @RequestBody(required = false) Map<String, Object> body) {

        log.info("Webhook MP recibido type={} dataId={}", type, dataId);
        webhookService.procesarWebhook(type, dataId);
        return ResponseEntity.ok().build();
    }

    // ══════════════════════════════════════════════════════════════════════
// TODO-DEPLOY: Este endpoint es PROVISIONAL para desarrollo local.
// En producción MP llama al /webhook automáticamente.
// Al desplegar: ELIMINAR este endpoint completo.
// ══════════════════════════════════════════════════════════════════════
    @PostMapping("/confirm/{reference}")
    public ResponseEntity<PaymentTransactionModel> confirmar(
            @PathVariable String reference) {

        log.info("[DEV] Confirmación manual solicitada ref={}", reference);

        PaymentTransactionModel tx = paymentService.getTransactionByReference(reference);

        // Si ya está completado, solo retornar
        if (tx.getStatus() == PaymentStatusModel.COMPLETED) {
            log.info("[DEV] Transacción {} ya estaba completada", reference);
            return ResponseEntity.ok(tx);
        }

        webhookService.confirmarManual(reference);
        return ResponseEntity.ok(paymentService.getTransactionByReference(reference));
    }


    @PostMapping("/membership")
    public ResponseEntity<PaymentResponseDTO> crearPagoMembresia(
            @RequestParam String userId,
            @RequestParam String plan,
            @RequestParam String email,
            @RequestParam String userName) {

        Map<String, Object> precios = Map.of(
                "FAN", 29.0, "PRO", 69.0, "ELITE", 149.0
        );

        double precio = (double) precios.getOrDefault(plan.toUpperCase(), 0.0);
        if (precio == 0) throw new RuntimeException("Plan inválido: " + plan);

        PaymentRequestDTO request = PaymentRequestDTO.builder()
                .userId(userId)
                .email(email)
                .userName(userName)
                .evenNombre("Membresía " + plan.toUpperCase() + " — EventPeru")
                .evenLugar("EventPeru")
                .eventId("MEMBERSHIP-" + plan.toUpperCase())
                .ticketType("MEMBERSHIP")
                .quantity(1)
                .unitPrice(BigDecimal.valueOf(precio))
                .build();

        return ResponseEntity.ok(paymentService.createPayment(request));
    }
}