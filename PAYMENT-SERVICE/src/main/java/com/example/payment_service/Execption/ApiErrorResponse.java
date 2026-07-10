package com.example.payment_service.Execption;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class ApiErrorResponse {

    // Información general del error de la API

    private LocalDateTime timestamp;
    private int status;
    private String errorCode;
    private String message;
    private String detail;
    private Map<String, String> details;
}
