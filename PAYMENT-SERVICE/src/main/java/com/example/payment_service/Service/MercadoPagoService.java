package com.example.payment_service.Service;

import com.example.payment_service.dto.Request.PaymentRequestDTO;
import com.mercadopago.resources.preference.Preference;

public interface MercadoPagoService {

    /**
     * Crea una preference en Mercado Pago y devuelve el init_point
     * para redirigir al usuario al checkout.
     */
    Preference crearPreferencia(PaymentRequestDTO request, String orderReference);
}
