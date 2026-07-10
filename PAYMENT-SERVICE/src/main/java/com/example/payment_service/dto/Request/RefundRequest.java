package com.example.payment_service.dto.Request;

import lombok.Data;

@Data
public class RefundRequest {
    private String orderReference;  // ORD-29AB4FDD
    private String motivo;          // "No puedo asistir", etc.
    private String detalles;        // descripción adicional
}
