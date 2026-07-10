package com.example.payment_service.Controller;


import com.example.payment_service.Service.AdminTicketService;
import com.example.payment_service.dto.Response.ReporteEventoResponse;
import com.example.payment_service.dto.Response.TicketAdminResponse;
import com.example.payment_service.dto.Response.TicketKpisResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tickets/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminTicketController {

    private final AdminTicketService adminTicketService;

    // ── KPIs ──────────────────────────────────────────────────────────────────
    @GetMapping("/kpis")
    public ResponseEntity<TicketKpisResponse> getKpis() {
        log.info("GET /tickets/admin/kpis");
        return ResponseEntity.ok(adminTicketService.getKpis());
    }

    // ── Listar paginado ───────────────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<Page<TicketAdminResponse>> listar(
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String eventoId,
            @RequestParam(defaultValue = "0")          int page,
            @RequestParam(defaultValue = "10")         int size,
            @RequestParam(defaultValue = "createdAt")  String sort,
            @RequestParam(defaultValue = "desc")       String direction) {

        log.info("GET /tickets/admin page={} size={} status={}", page, size, status);

        Sort sortBy = direction.equalsIgnoreCase("desc")
                ? Sort.by(sort).descending()
                : Sort.by(sort).ascending();

        return ResponseEntity.ok(adminTicketService.listar(
                busqueda, status, eventoId,
                PageRequest.of(page, size, sortBy)));
    }

    // ── Detalle ───────────────────────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<TicketAdminResponse> getDetalle(@PathVariable String id) {
        log.info("GET /tickets/admin/{}", id);
        return ResponseEntity.ok(adminTicketService.getDetalle(id));
    }

    // ── Cancelar ──────────────────────────────────────────────────────────────
    @PutMapping("/{id}/cancelar")
    public ResponseEntity<TicketAdminResponse> cancelar(@PathVariable String id) {
        log.info("PUT /tickets/admin/{}/cancelar", id);
        return ResponseEntity.ok(adminTicketService.cancelar(id));
    }

    @GetMapping("/eventos-con-tickets")
    public ResponseEntity<List<Map<String, Object>>> getEventosConTickets() {
        return ResponseEntity.ok(adminTicketService.getEventosConTickets());
    }

    // ── Reporte por evento ────────────────────────────────────────────────────
    @GetMapping("/reporte/{eventoId}")
    public ResponseEntity<ReporteEventoResponse> getReporte(@PathVariable String eventoId) {
        log.info("GET /tickets/admin/reporte/{}", eventoId);
        return ResponseEntity.ok(adminTicketService.getReporteEvento(eventoId));
    }

    // ── Exportar Excel ────────────────────────────────────────────────────────
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportarExcel(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String eventoId) {

        log.info("GET /tickets/admin/export status={} eventoId={}", status, eventoId);

        byte[] excel = adminTicketService.exportarExcel(status, eventoId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "tickets.xlsx");

        return ResponseEntity.ok().headers(headers).body(excel);
    }
}