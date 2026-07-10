package com.example.Notification_Service.Controller;

import com.example.Notification_Service.Service.Implements.PreferenciasService;
import com.example.Notification_Service.dto.Request.PreferenciasRequest;
import com.example.Notification_Service.dto.Response.PreferenciasResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications/usuario/{usuarioId}/preferencias")
@RequiredArgsConstructor
public class PreferenciasController {

    private final PreferenciasService preferenciasService;

    @GetMapping
    public ResponseEntity<PreferenciasResponse> obtener(@PathVariable String usuarioId) {
        return ResponseEntity.ok(preferenciasService.obtener(usuarioId));
    }

    @PutMapping
    public ResponseEntity<PreferenciasResponse> guardar(
            @PathVariable String usuarioId,
            @RequestBody PreferenciasRequest request) {
        return ResponseEntity.ok(preferenciasService.guardar(usuarioId, request));
    }
}
