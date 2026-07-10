package com.example.payment_service.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "comprobantes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComprobanteModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String numeroComprobante; // B001-1, F001-1

    @Column(nullable = false)
    private String tipoComprobante; // boleta, factura

    @Column(nullable = false)
    private String transactionReference;

    @Column(nullable = false)
    private String usuarioId;

    @Column(nullable = false)
    private String clienteNombre;

    @Column(nullable = false)
    private String clienteDocumento;

    @Column(nullable = false)
    private String clienteEmail;

    @Column(nullable = false)
    private String eventoNombre;

    private Double totalVenta;

    @Column(nullable = false)
    private String sunatCode;

    @Column(nullable = false)
    private String sunatMensaje;

    private Boolean aceptadoSunat;

    @Column(columnDefinition = "TEXT")
    private String xmlFirmado; // XML en base64

    @Column(columnDefinition = "TEXT")
    private String pdfBase64;

    @CreationTimestamp
    private LocalDateTime fechaEmision;
}