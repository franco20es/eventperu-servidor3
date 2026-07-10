package com.example.payment_service.dto.Request;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class BillingRequest {
    private String tipoComprobante; // "boleta" o "factura"
    private String transactionReference;
    private String clienteNombre;
    private String usuarioId;
    private String clienteDocumento;
    private String clienteTipoDoc;  // "1" DNI, "6" RUC
    private String clienteEmail;
    private String clienteDireccion;
    private String razonSocial;
    private String eventoNombre;
    private String eventoFecha;
    private String eventoLugar;
    private List<BillingItemRequest> items;
}
