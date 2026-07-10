package com.example.payment_service.Mapper;

import com.example.payment_service.Model.PaymentStatusModel;
import org.springframework.stereotype.Component;

//mapea los estados de Culqi a los estados de nuestro sistema
@Component
public class CulqiMapper {

    public PaymentStatusModel mapStatus(String status) {
        if (status == null) return PaymentStatusModel.PENDING;

        return switch (status.toLowerCase()) {
            case "venta_exitosa", "completado", "paid", "approved" ->
                    PaymentStatusModel.COMPLETED;

            case "rechazado", "failed", "declined" ->
                    PaymentStatusModel.FAILED;

            case "cancelado", "refunded" ->
                    PaymentStatusModel.CANCELLED;

            default -> PaymentStatusModel.PENDING;
        };
    }

}
