package com.example.payment_service.dto.Response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RefundResponse {
    private boolean success;
    private String  orderReference;
    private String  mpRefundId;
    private Double  monto;
    private String  motivo;
    private String  mensaje;
}
