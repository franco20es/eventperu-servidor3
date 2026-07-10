package com.example.payment_service.Model;

// Enum para representar el estado de una transacción de pago

public enum PaymentStatusModel {

    PENDING("Pendiente de pago"),
    COMPLETED("Pago realizado"),
    FAILED("Pago rechazado"),
    CANCELLED("Pago anulado"),
    REFUNDED("Pago reembolsado");

    private final String description;

    PaymentStatusModel(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
