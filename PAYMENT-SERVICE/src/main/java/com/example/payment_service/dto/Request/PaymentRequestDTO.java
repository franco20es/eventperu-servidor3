package com.example.payment_service.dto.Request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor // ← agregar
@AllArgsConstructor
public class PaymentRequestDTO {

 // ── Usuario ────────────────────────────────────────────────────────
 @NotBlank(message = "userId es requerido")
 private String userId;

 @NotBlank(message = "email es requerido")
 @Email(message = "Email inválido")
 private String email;

 @NotBlank(message = "userName es requerido")
 private String userName;

 private String firstName;
 private String lastName;
 private String phoneNumber;

 // ── Evento ─────────────────────────────────────────────────────────
 @NotBlank(message = "eventId es requerido")
 private String eventId;

 @NotBlank(message = "evenNombre es requerido")
 private String evenNombre;        // nombre del evento

 @NotBlank(message = "evenLugar es requerido")
 private String evenLugar;         // lugar del evento

 @NotBlank(message = "evenFecha es requerido")
 private String evenFecha;         // "2025-05-24"

 private LocalDateTime eventDate;  // para crear el ticket

 // ── Entradas ───────────────────────────────────────────────────────
 @NotBlank(message = "ticketType es requerido")
 private String ticketType;

 @NotNull(message = "quantity es requerido")
 @Min(value = 1, message = "quantity debe ser mayor a 0")
 private Integer quantity;

 @NotNull(message = "unitPrice es requerido")
 @DecimalMin(value = "0.01", message = "unitPrice debe ser mayor a 0")
 private BigDecimal unitPrice;

 // ── Comprobante SUNAT (sin cambios) ────────────────────────────────
 @NotBlank(message = "tipoComprobante es requerido (boleta o factura)")
 @Pattern(regexp = "^(boleta|factura)$")
 private String tipoComprobante;

 @NotBlank(message = "clienteDocumento es requerido")
 private String clienteDocumento;

 @NotBlank(message = "clienteTipoDoc es requerido (1=DNI, 6=RUC)")
 private String clienteTipoDoc;

 private String razonSocial;       // solo si factura

 @NotNull(message = "items es requerido")
 @NotEmpty(message = "items no puede estar vacío")
 private List<BillingItemRequest> items;

 private String zonaId;  // ← agregar

 // ELIMINADO: sourceId, amount(Double), currency, installments,
 //            address, city, countryCode, description
}