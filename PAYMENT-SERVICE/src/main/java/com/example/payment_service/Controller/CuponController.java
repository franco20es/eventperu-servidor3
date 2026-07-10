package com.example.payment_service.Controller;


import com.example.payment_service.Service.CuponService;
import com.example.payment_service.dto.Request.CuponRequest;
import com.example.payment_service.dto.Request.ValidarCuponRequest;
import com.example.payment_service.dto.Response.CuponKpisResponse;
import com.example.payment_service.dto.Response.CuponResponse;
import com.example.payment_service.dto.Response.ValidarCuponResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cupones")
@RequiredArgsConstructor
@Slf4j
public class CuponController {

    private final CuponService cuponService;

    // ── KPIs ──────────────────────────────────────────────────────────────────
    @GetMapping("/kpis")
    public ResponseEntity<CuponKpisResponse> getKpis() {
        log.info("GET /cupones/kpis");
        return ResponseEntity.ok(cuponService.getKpis());
    }

    // ── Listar paginado ───────────────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<Page<CuponResponse>> listar(
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) String estado,
            @RequestParam(defaultValue = "0")          int page,
            @RequestParam(defaultValue = "10")         int size,
            @RequestParam(defaultValue = "createdAt")  String sort,
            @RequestParam(defaultValue = "desc")       String direction) {

        log.info("GET /cupones page={} size={} estado={}", page, size, estado);

        Sort sortBy = direction.equalsIgnoreCase("desc")
                ? Sort.by(sort).descending()
                : Sort.by(sort).ascending();

        return ResponseEntity.ok(
                cuponService.listar(busqueda, estado, PageRequest.of(page, size, sortBy)));
    }

    // ── Obtener por ID ────────────────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<CuponResponse> obtener(@PathVariable String id) {
        log.info("GET /cupones/{}", id);
        return ResponseEntity.ok(cuponService.obtener(id));
    }

    // ── Crear ─────────────────────────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<CuponResponse> crear(
            @Valid @RequestBody CuponRequest request,
            Authentication auth) {
        String emailAdmin = auth != null ? auth.getName() : "admin";
        log.info("POST /cupones codigo={} por={}", request.getCodigo(), emailAdmin);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cuponService.crear(request, emailAdmin));
    }

    // ── Actualizar ────────────────────────────────────────────────────────────
    @PutMapping("/{id}")
    public ResponseEntity<CuponResponse> actualizar(
            @PathVariable String id,
            @Valid @RequestBody CuponRequest request) {
        log.info("PUT /cupones/{}", id);
        return ResponseEntity.ok(cuponService.actualizar(id, request));
    }

    // ── Activar ───────────────────────────────────────────────────────────────
    @PutMapping("/{id}/activar")
    public ResponseEntity<CuponResponse> activar(@PathVariable String id) {
        log.info("PUT /cupones/{}/activar", id);
        return ResponseEntity.ok(cuponService.activar(id));
    }

    // ── Desactivar ────────────────────────────────────────────────────────────
    @PutMapping("/{id}/desactivar")
    public ResponseEntity<CuponResponse> desactivar(@PathVariable String id) {
        log.info("PUT /cupones/{}/desactivar", id);
        return ResponseEntity.ok(cuponService.desactivar(id));
    }

    // ── Eliminar ──────────────────────────────────────────────────────────────
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        log.info("DELETE /cupones/{}", id);
        cuponService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    // ── Validar (sin aplicar) — usado en checkout ─────────────────────────────
    @PostMapping("/validar")
    public ResponseEntity<ValidarCuponResponse> validar(
            @Valid @RequestBody ValidarCuponRequest request) {
        log.info("POST /cupones/validar codigo={}", request.getCodigo());
        return ResponseEntity.ok(cuponService.validar(request));
    }

    // ── Aplicar (valida + incrementa uso) — usado al confirmar pago ───────────
    @PostMapping("/aplicar")
    public ResponseEntity<ValidarCuponResponse> aplicar(
            @Valid @RequestBody ValidarCuponRequest request) {
        log.info("POST /cupones/aplicar codigo={}", request.getCodigo());
        return ResponseEntity.ok(cuponService.aplicar(request));
    }
}