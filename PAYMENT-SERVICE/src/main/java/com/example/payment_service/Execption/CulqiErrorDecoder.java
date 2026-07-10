package com.example.payment_service.Execption;

import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;

// Decodificador de errores personalizado 
// para manejar respuestas de error de la API de Culqi
@Slf4j
public class CulqiErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {

        log.error("Error Culqi → status={}", response.status());

        String message = switch (response.status()) {
            case 400 -> "Solicitud inválida a Culqi";
            case 401 -> "Credenciales inválidas";
            case 402 -> "Pago rechazado o fondos insuficientes";
            case 403 -> "Acceso denegado a Culqi";
            case 422 -> "Validación fallida en Culqi";
            default -> "Error en pasarela de pagos";
        };

        return new PaymentException(
                "CULQI_ERROR",
                message,
                message
        );
    }
}