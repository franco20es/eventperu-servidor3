package com.example.payment_service.Execption;

import lombok.Getter;

// Excepción personalizada para 
// errores relacionados con el procesamiento de pagos

@Getter
public class PaymentException extends RuntimeException {

    private final String errorCode;
    private final String userMessage;


    // Constructores para diferentes escenarios de error

    public PaymentException(String message) {
        super(message);
        this.errorCode = "PAYMENT_ERROR";
        this.userMessage = message;
    }

    public PaymentException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.userMessage = message;
    }

    public PaymentException(String message, String errorCode, String userMessage) {
        super(message);
        this.errorCode = errorCode;
        this.userMessage = userMessage;
    }

    // Errores predefinidos
    public static PaymentException notFound(String transactionReference) {
        return new PaymentException(
                "Transacción no encontrada: " + transactionReference,
                "PAYMENT_NOT_FOUND",
                "No se encontró el pago solicitado"
        );
    }

    public static PaymentException culqiError(String culqiMessage) {
        return new PaymentException(
                "Error en Culqi: " + culqiMessage,
                "CULQI_ERROR",
                "Error al procesar el pago. Intenta nuevamente"
        );
    }

    public static PaymentException invalidToken() {
        return new PaymentException(
                "Token de pago inválido",
                "INVALID_TOKEN",
                "El token de pago es inválido. Por favor, intenta nuevamente"
        );
    }
}
