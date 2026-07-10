package com.example.payment_service.dto.Response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SunatResponse {
    private boolean success;
    private String code;
    private String message;
    private String cdrBase64;
}