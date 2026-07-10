package com.example.payment_service.Client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "event-service")
public interface EventServiceClient {

    // Reduce disponibles de una zona específica
    @PutMapping("/api/v1/eventos/{eventoId}/zonas/{zonaId}/reducir")
    void reducirDisponibles(
            @PathVariable String eventoId,
            @PathVariable String zonaId,
            @RequestParam int cantidad
    );

    // Actualiza capacidadDisponible del evento
    @PutMapping("/api/v1/eventos/{eventoId}/reducir-capacidad")
    void reducirCapacidad(
            @PathVariable String eventoId,
            @RequestParam int cantidad
    );
}