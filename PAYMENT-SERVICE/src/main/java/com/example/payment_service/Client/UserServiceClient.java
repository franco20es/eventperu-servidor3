package com.example.payment_service.Client;

import com.example.payment_service.dto.Response.UsuarioResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "user-service")
public interface UserServiceClient {

    @GetMapping("/api/v1/memberships/descuento/{userId}")
    Map<String, Integer> obtenerDescuento(@PathVariable String userId);

    @PostMapping("/api/v1/memberships/activar")
    void activarMembresia(
            @RequestParam String userId,
            @RequestParam String plan,
            @RequestParam String txnRef);

    @GetMapping("/api/v1/users/internal/{id}")
    UsuarioResponse obtenerUsuario(@PathVariable("id") String email);
}
