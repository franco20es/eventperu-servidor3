package com.example.payment_service.Service.Implements;

import com.example.payment_service.Model.TicketModel;
import com.example.payment_service.Model.TicketStatus;
import com.example.payment_service.Repository.TicketRepository;
import com.example.payment_service.Service.TicketService;
import com.example.payment_service.Util.PdfTicketUtil;
import com.example.payment_service.dto.Response.TicketValidationResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import org.springframework.data.domain.Pageable;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final PdfTicketUtil    pdfTicketUtil;

    @Value("${jwt.secret}")
    private String jwtSecret;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ── Listar tickets de usuario ──────────────────────────────────────────
//    @Override
//    public List<TicketModel> getTicketsByUser(String userId) {
//        return ticketRepository.findByUserIdOrderByCreatedAtDesc(userId);
//    }
    @Override
    public Page<TicketModel> getTicketsByUser(
            String userId,
            Pageable pageable
    ) {
        return ticketRepository.findByUserIdOrderByCreatedAtDesc(
                userId,
                pageable
        );
    }

    // ── Obtener por código ─────────────────────────────────────────────────
    @Override
    public TicketModel getByCode(String code) {
        return ticketRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado: " + code));
    }

    // ── CHECK (solo lectura, sin marcar usado) ─────────────────────────────
    @Override
    @Transactional
    public TicketValidationResponse checkTicketStatus(String qrToken) {
        log.info("Consultando estado de ticket (Solo Lectura)...");

        // Extraer JWT si viene con URL completa
        String jwt = extraerJwt(qrToken);

        Claims claims;
        try {
            claims = extractClaims(jwt);
        } catch (Exception e) {
            log.warn("Token QR de consulta inválido: {}", e.getMessage());
            return invalido("QR inválido o falsificado");
        }

        String ticketCode = claims.getSubject();
        TicketModel ticket = ticketRepository.findByCode(ticketCode).orElse(null);

        if (ticket == null) return invalido("Ticket no encontrado");

        if (ticket.getUsed()) {
            return TicketValidationResponse.builder()
                    .valid(false).used(true)
                    .message("Ticket ya utilizado el " + ticket.getUsedAt().format(FMT))
                    .ticketCode(ticket.getCode())
                    .ownerName(ticket.getUserName())
                    .build();
        }

        if (ticket.getStatus() != TicketStatus.ACTIVE)
            return invalido("Ticket no activo: " + ticket.getStatus());

        return TicketValidationResponse.builder()
                .valid(true).used(false)
                .message("Ticket válido para ingresar")
                .ticketCode(ticket.getCode())
                .ownerName(ticket.getUserName())
                .ownerEmail(ticket.getUserEmail())
                .eventName(ticket.getEventName())
                .eventLocation(ticket.getEventLocation())
                .eventDate(formatFecha(ticket))
                .ticketType(ticket.getTicketType())
                .build();
    }

    @Override
    public Page<TicketModel> getTicketsValidados(String eventId, Pageable pageable) {
        if (eventId != null && !eventId.isBlank()) {
            return ticketRepository.findByStatusAndEventIdOrderByUsedAtDesc(
                    TicketStatus.USED, eventId, pageable);
        }
        return ticketRepository.findByStatusOrderByUsedAtDesc(TicketStatus.USED, pageable);
    }


    // ── VALIDATE (marca como usado) ────────────────────────────────────────
    @Override
    @Transactional
    public TicketValidationResponse validateTicket(String qrToken) {
        log.info("Validando ticket... qrToken recibido: '{}'", qrToken);

        // ── Modo 1: código directo EVT-XXXX ──────────────────────────────
        if (qrToken != null && qrToken.trim().toUpperCase().startsWith("EVT-")) {
            return validarPorCodigo(qrToken.trim().toUpperCase());
        }

        // ── Modo 2: URL completa o JWT ────────────────────────────────────
        String jwt = extraerJwt(qrToken);

        Claims claims;
        try {
            claims = extractClaims(jwt);
        } catch (Exception e) {
            log.warn("Token QR inválido: {}", e.getMessage());
            return invalido("QR inválido o falsificado");
        }

        String ticketCode = claims.getSubject();
        return validarPorCodigo(ticketCode);
    }

    // ── Lógica compartida de validación ───────────────────────────────────
    private TicketValidationResponse validarPorCodigo(String ticketCode) {
        TicketModel ticket = ticketRepository.findByCode(ticketCode).orElse(null);

        if (ticket == null) return invalido("Ticket no encontrado: " + ticketCode);

        if (ticket.getUsed()) {
            log.warn("Ticket ya utilizado: {}", ticketCode);
            return TicketValidationResponse.builder()
                    .valid(false).used(true)
                    .message("Ticket ya utilizado el " + ticket.getUsedAt().format(FMT))
                    .ticketCode(ticket.getCode())
                    .ownerName(ticket.getUserName())
                    .build();
        }

        if (ticket.getStatus() != TicketStatus.ACTIVE) {
            return TicketValidationResponse.builder()
                    .valid(false).used(false)
                    .message("Ticket no activo: " + ticket.getStatus())
                    .ticketCode(ticket.getCode())
                    .build();
        }

        // Marcar como usado
        ticket.setUsed(true);
        ticket.setUsedAt(LocalDateTime.now());
        ticket.setStatus(TicketStatus.USED);
        ticketRepository.save(ticket);

        log.info("Ticket validado y marcado como usado: {}", ticketCode);

        return TicketValidationResponse.builder()
                .valid(true).used(false)
                .message("Acceso permitido")
                .ticketCode(ticket.getCode())
                .ownerName(ticket.getUserName())
                .ownerEmail(ticket.getUserEmail())
                .eventName(ticket.getEventName())
                .eventLocation(ticket.getEventLocation())
                .eventDate(formatFecha(ticket))
                .ticketType(ticket.getTicketType())
                .build();
    }




    // ── PDF ────────────────────────────────────────────────────────────────
    @Override
    public byte[] generateTicketPdf(String ticketId) {
        TicketModel ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado: " + ticketId));
        return pdfTicketUtil.generateTicketPdf(ticket);
    }

    // ── Helpers ────────────────────────────────────────────────────────────
    private String extraerJwt(String qrToken) {
        if (qrToken != null && qrToken.contains("?token=")) {
            return qrToken.substring(qrToken.indexOf("?token=") + 7);
        }
        return qrToken;
    }

    private TicketValidationResponse invalido(String msg) {
        return TicketValidationResponse.builder()
                .valid(false).used(false).message(msg).build();
    }

    private String formatFecha(TicketModel ticket) {
        return ticket.getEventDate() != null
                ? ticket.getEventDate().format(FMT) : null;
    }

    private Claims extractClaims(String token) {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 64) {
            byte[] padded = new byte[64];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            keyBytes = padded;
        }
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}