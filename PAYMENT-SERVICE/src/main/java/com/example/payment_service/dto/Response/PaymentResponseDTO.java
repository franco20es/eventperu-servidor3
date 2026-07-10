package com.example.payment_service.dto.Response;

import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class PaymentResponseDTO {

       // URL donde el frontend redirige al usuario para pagar
       private String checkoutUrl;       // init_point de Mercado Pago

       // ID de la preference (para el SDK JS de MP si lo usas)
       private String preferenceId;

       // Referencia interna de la transacción
       private String transactionReference;

       // Estado inicial — siempre PENDING hasta que el webhook confirme
       private String status;
       private String message;

       // Resumen del pedido para mostrar al usuario
       private String eventName;
       private Integer quantity;
       private Double totalAmount;

       private String zonaId;  // ← agregar
}
