package com.example.payment_service.dto.Response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BillingResponse {
    private boolean success;
    private String sunatCode;
    private String message;
    private String cdr;
    private String xmlSigned;
    private String numeroComprobante;
    private String pdfBase64;
}
