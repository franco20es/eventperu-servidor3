package com.example.payment_service.dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransaccionResponse {
    private String  id;
    private String  referencia;
    private String  comprador;
    private String  email;
    private String  evento;
    private String  eventoId;
    private String  zona;
    private Integer tickets;
    private String  monto;        // "S/ 150.00"
    private double  montoNum;
    private String  comision;     // "S/ 15.00"
    private double  comisionNum;
    private String  metodo;       // "Mercado Pago"
    private String  fecha;        // ISO string
    private String  estado;       // "EXITOSO" | "FALLIDO" | "REEMBOLSADO" | "PENDIENTE"
}
