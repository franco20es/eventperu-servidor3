package com.example.payment_service.Controller;



import com.example.payment_service.Service.AdminPaymentService;
import com.example.payment_service.Service.PaymentStatsService;
import com.example.payment_service.dto.Response.IngresoEventoResponse;
import com.example.payment_service.dto.Response.IngresoMesResponse;
import com.example.payment_service.dto.Response.PagoKpisResponse;
import com.example.payment_service.dto.Response.TransaccionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminPaymentController {

    private final AdminPaymentService adminPaymentService;


    // ── KPIs ──────────────────────────────────────────────────────────────────
    @GetMapping("/kpis")
    public ResponseEntity<PagoKpisResponse> getKpis() {
        log.info("GET /payments/admin/kpis");
        return ResponseEntity.ok(adminPaymentService.getKpis());
    }

    // ── Listar transacciones paginado ─────────────────────────────────────────
    @GetMapping
    public ResponseEntity<Page<TransaccionResponse>> listar(
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String eventoId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(defaultValue = "0")    int page,
            @RequestParam(defaultValue = "10")   int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction) {

        log.info("GET /payments/admin page={} size={} estado={} eventoId={}", page, size, estado, eventoId);

        Sort sortBy = direction.equalsIgnoreCase("desc")
                ? Sort.by(sort).descending()
                : Sort.by(sort).ascending();

        Page<TransaccionResponse> resultado = adminPaymentService.listarTransacciones(
                busqueda, estado, eventoId, fecha,
                PageRequest.of(page, size, sortBy));

        return ResponseEntity.ok(resultado);
    }

    // ── Ingresos por mes ──────────────────────────────────────────────────────
    @GetMapping("/ingresos/mes")
    public ResponseEntity<List<IngresoMesResponse>> ingresosPorMes(
            @RequestParam(defaultValue = "2026") int anio) {
        log.info("GET /payments/admin/ingresos/mes anio={}", anio);
        return ResponseEntity.ok(adminPaymentService.getIngresosPorMes(anio));
    }

    // ── Ingresos por evento ───────────────────────────────────────────────────
    @GetMapping("/ingresos/evento")
    public ResponseEntity<List<IngresoEventoResponse>> ingresosPorEvento() {
        log.info("GET /payments/admin/ingresos/evento");
        return ResponseEntity.ok(adminPaymentService.getIngresosPorEvento());
    }

    // ── Detalle de transacción ────────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<TransaccionResponse> getDetalle(@PathVariable String id) {
        log.info("GET /payments/admin/{}", id);
        return ResponseEntity.ok(adminPaymentService.getDetalle(id));
    }

    // ── Exportar Excel ────────────────────────────────────────────────────────
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportarExcel(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String eventoId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {

        log.info("GET /payments/admin/export estado={} eventoId={}", estado, eventoId);

        byte[] excel = adminPaymentService.exportarExcel(estado, eventoId, fecha);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "transacciones.xlsx");

        return ResponseEntity.ok().headers(headers).body(excel);
    }




}