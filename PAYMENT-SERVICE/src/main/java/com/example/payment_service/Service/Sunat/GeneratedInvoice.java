package com.example.payment_service.Service.Sunat;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GeneratedInvoice {
    private String xml;
    private String numero;
    private String serie;
    private String correlativo;
    private String tipoDoc;
    private double totalVenta;
}
