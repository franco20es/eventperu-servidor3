package com.example.payment_service.Controller;

import com.example.payment_service.Model.TicketModel;
import com.example.payment_service.Service.TicketService;
import com.example.payment_service.dto.Request.ValidateTicketRequest;
import com.example.payment_service.dto.Response.TicketValidationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
@Slf4j
public class TicketController {

    private final TicketService ticketService;

    // ── Usuario ve sus tickets ─────────────────────────────────────────
//    @GetMapping("/user/{userId}")
//    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_ADMIN')")
//    public ResponseEntity<List<TicketModel>> getTicketsByUser(
//            @PathVariable String userId) {
//        log.info("Obteniendo tickets de usuario: {}", userId);
//        return ResponseEntity.ok(ticketService.getTicketsByUser(userId));
//    }
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_ADMIN')")
    public ResponseEntity<Page<TicketModel>> getTicketsByUser(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("Obteniendo tickets de usuario: {} página: {}", userId, page);

        return ResponseEntity.ok(
                ticketService.getTicketsByUser(
                        userId,
                        PageRequest.of(page, size)
                )
        );
    }
    // ── Ver ticket individual ──────────────────────────────────────────
    @GetMapping("/{code}")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_ADMIN','ROLE_STAFF')")
    public ResponseEntity<TicketModel> getTicket(
            @PathVariable String code) {
        log.info("Obteniendo ticket: {}", code);
        return ResponseEntity.ok(ticketService.getByCode(code));
    }

    // ── Check QR — público (el usuario ve su QR sin marcarlo usado) ────
    @GetMapping("/check")
    public ResponseEntity<TicketValidationResponse> checkTicketStatus(
            @RequestParam("token") String qrToken) {
        log.info("Consultando estado de ticket (sin consumir)...");
        return ResponseEntity.ok(ticketService.checkTicketStatus(qrToken));
    }

    // ── Validar ticket — SOLO STAFF o ADMIN ───────────────────────────
    @PostMapping("/validate")
    @PreAuthorize("hasAnyAuthority('ROLE_STAFF','ROLE_ADMIN')")
    public ResponseEntity<TicketValidationResponse> validateTicket(
            @RequestBody ValidateTicketRequest request) {
        log.info("Staff validando ticket...");
        return ResponseEntity.ok(ticketService.validateTicket(request.getQrToken()));
    }

    // ── Descargar PDF ──────────────────────────────────────────────────
    @GetMapping("/{ticketId}/pdf")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_ADMIN')")
    public ResponseEntity<byte[]> downloadPdf(
            @PathVariable String ticketId) {
        log.info("Descargando PDF ticket: {}", ticketId);
        byte[] pdf = ticketService.generateTicketPdf(ticketId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=ticket-" + ticketId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // ── Historial de validaciones — STAFF o ADMIN ──────────────────
    @GetMapping("/validados")
    @PreAuthorize("hasAnyAuthority('ROLE_STAFF','ROLE_ADMIN')")
    public ResponseEntity<Page<TicketModel>> getTicketsValidados(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false)    String eventId) {
        log.info("Obteniendo historial de tickets validados");
        return ResponseEntity.ok(ticketService.getTicketsValidados(eventId, PageRequest.of(page, size)));
    }
}