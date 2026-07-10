package com.example.payment_service.dto.Request;



import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ValidateTicketRequest {

    @NotBlank(message = "El token QR es obligatorio")
    private String qrToken;
}