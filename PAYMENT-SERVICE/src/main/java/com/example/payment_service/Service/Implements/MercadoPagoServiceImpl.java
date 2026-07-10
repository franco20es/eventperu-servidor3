package com.example.payment_service.Service.Implements;

import com.example.payment_service.Client.UserServiceClient;
import com.example.payment_service.Service.MercadoPagoService;
import com.example.payment_service.dto.Request.PaymentRequestDTO;
import com.mercadopago.client.preference.*;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.preference.Preference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MercadoPagoServiceImpl implements MercadoPagoService {

    @Value("${app.frontend-url}")
    private String frontendUrl;

    private final UserServiceClient userServiceClient;

    @Override
    public Preference crearPreferencia(PaymentRequestDTO request, String orderReference) {
        log.info("=== frontendUrl: {}", frontendUrl);
        try {

            // ── Descuento por membresía ────────────────────────────────
            BigDecimal precioFinal = request.getUnitPrice();
            int descuentoPct = 0;

            if (request.getUserId() != null && !"MEMBERSHIP".equals(request.getTicketType())) {
                try {
                    Map<String, Integer> desc = userServiceClient.obtenerDescuento(request.getUserId());
                    descuentoPct = desc.getOrDefault("descuentoPct", 0);
                    if (descuentoPct > 0) {
                        BigDecimal descuento = precioFinal
                                .multiply(BigDecimal.valueOf(descuentoPct))
                                .divide(BigDecimal.valueOf(100));
                        precioFinal = precioFinal.subtract(descuento);
                        log.info("Descuento membresía {}%: {} → {}", descuentoPct,
                                request.getUnitPrice(), precioFinal);
                    }
                } catch (Exception e) {
                    log.warn("No se pudo consultar descuento membresía: {}", e.getMessage());
                }
            }

            // ── Item ───────────────────────────────────────────────────
            String titulo = request.getEvenNombre() + " — " + request.getTicketType();
            if (descuentoPct > 0) {
                titulo += " (-" + descuentoPct + "%)";
            }

            PreferenceItemRequest item = PreferenceItemRequest.builder()
                    .id(request.getEventId())
                    .title(titulo)
                    .description("Entrada para " + request.getUserName()
                            + " en " + request.getEvenLugar())
                    .quantity(request.getQuantity())
                    .unitPrice(precioFinal)
                    .currencyId("PEN")
                    .build();

            // ── Comprador ──────────────────────────────────────────────
            PreferencePayerRequest payer = PreferencePayerRequest.builder()
                    .name(request.getFirstName())
                    .surname(request.getLastName())
                    .email(request.getEmail())
                    .build();

            // ── URLs de retorno ────────────────────────────────────────
            String baseUrl = frontendUrl.trim().replaceAll("/+$", "");

            PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                    .success(baseUrl + "/pago/exitoso")
                    .failure(baseUrl + "/pago/fallido")
                    .pending(baseUrl + "/pago/pendiente")
                    .build();

            log.info("back_urls success=[{}] failure=[{}] pending=[{}]",
                    baseUrl + "/pago/exitoso",
                    baseUrl + "/pago/fallido",
                    baseUrl + "/pago/pendiente");

            // ── Preference ─────────────────────────────────────────────
            PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                    .items(List.of(item))
                    .payer(payer)
                    .backUrls(backUrls)
                    .autoReturn("approved")
                    .externalReference(orderReference)
                    .statementDescriptor("EVENTPERU")
                    .build();

            Preference preference = new PreferenceClient().create(preferenceRequest);
            log.info("Preference creada id={} ref={} precio={}",
                    preference.getId(), orderReference, precioFinal);
            return preference;

        } catch (MPApiException e) {
            log.error("MP API error status={} body={}",
                    e.getStatusCode(), e.getApiResponse().getContent());
            throw new RuntimeException("Error en Mercado Pago: " + e.getMessage());
        } catch (MPException e) {
            log.error("MP error: {}", e.getMessage());
            throw new RuntimeException("Error al conectar con Mercado Pago");
        }
    }
}