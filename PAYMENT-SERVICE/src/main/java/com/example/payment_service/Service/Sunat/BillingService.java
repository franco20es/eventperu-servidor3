package com.example.payment_service.Service.Sunat;

import com.example.payment_service.Client.NotificationClient;
import com.example.payment_service.Model.ComprobanteModel;
import com.example.payment_service.Repository.ComprobanteRepository;
import com.example.payment_service.Util.PdfBoletaUtil;
import com.example.payment_service.Util.PdfFacturaUtil;
import com.example.payment_service.dto.Request.BillingRequest;
import com.example.payment_service.dto.Request.TicketEmailRequest;
import com.example.payment_service.dto.Response.BillingResponse;
import com.example.payment_service.dto.Response.SunatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillingService {

    private final UblInvoiceGenerator xmlGenerator;
    private final XmlSignerService xmlSigner;
    private final SunatSoapClient sunatClient;
    private final PdfService pdfService;
    private final PdfFacturaUtil pdfFacturaUtil;
    private final ComprobanteRepository comprobanteRepository;
    private final PdfBoletaUtil pdfBoletaUtil;
    private final NotificationClient notificationClient;

    @Value("${sunat.ruc}")
    private String ruc;

    @Value("${sunat.razon-social}")
    private String razonSocial;

    public BillingResponse emitirComprobante(BillingRequest request) {
        try {
            log.info("Iniciando emisión de {} para: {}",
                    request.getTipoComprobante(), request.getClienteNombre());

            // 1. Generar XML UBL 2.1
            GeneratedInvoice generated = xmlGenerator.generateXml(request, ruc, razonSocial);
            log.info("XML generado: {}", generated.getNumero());

            // 2. Firmar XML
            String xmlSigned = xmlSigner.signXml(generated.getXml());
            log.info("XML firmado correctamente");

            // 3. Enviar a SUNAT
            String fileName = ruc + "-" + generated.getTipoDoc()
                    + "-" + generated.getSerie()
                    + "-" + generated.getCorrelativo();

            SunatResponse sunatResponse = sunatClient.sendBill(xmlSigned, fileName);
            log.info("SUNAT respuesta: {} - {}",
                    sunatResponse.getCode(), sunatResponse.getMessage());

            boolean exitoso = sunatResponse.isSuccess() ||
                    (sunatResponse.getCode() != null &&
                            sunatResponse.getCode().contains("2074"));

            // 4. Construir respuesta
            BillingResponse billingResponse = BillingResponse.builder()
                    .success(exitoso)
                    .sunatCode(exitoso ? "0" : sunatResponse.getCode())
                    .message(exitoso
                            ? "Comprobante generado y firmado digitalmente - Enviado a SUNAT"
                            : sunatResponse.getMessage())
                    .numeroComprobante(generated.getNumero())
                    .cdr(sunatResponse.getCdrBase64())
                    .xmlSigned(Base64.getEncoder().encodeToString(xmlSigned.getBytes()))
                    .build();

            // 5. Generar PDF base64 interno
            String pdfBase64 = pdfService.generarPdfBase64(request, billingResponse);
            billingResponse.setPdfBase64(pdfBase64);

            // 6. Guardar en BD
            ComprobanteModel comprobante = ComprobanteModel.builder()
                    .numeroComprobante(generated.getNumero())
                    .tipoComprobante(request.getTipoComprobante())
                    .transactionReference(request.getTransactionReference())
                    .usuarioId(request.getUsuarioId())
                    .clienteNombre(request.getClienteNombre())
                    .clienteDocumento(request.getClienteDocumento())
                    .clienteEmail(request.getClienteEmail())
                    .eventoNombre(request.getEventoNombre())
                    .totalVenta(generated.getTotalVenta())
                    .sunatCode(billingResponse.getSunatCode())
                    .sunatMensaje(billingResponse.getMessage())
                    .aceptadoSunat(exitoso)
                    .xmlFirmado(billingResponse.getXmlSigned())
                    .pdfBase64(pdfBase64)
                    .build();

            comprobanteRepository.save(comprobante);
            log.info("Comprobante guardado en BD: {}", generated.getNumero());

            // 7. Generar PDF boleta/factura profesional y enviar por email
            try {
                byte[] pdfComprobante;
                String tipoDoc;

                if ("factura".equalsIgnoreCase(request.getTipoComprobante())) {
                    pdfComprobante = pdfFacturaUtil.generateFacturaPdf(comprobante, request);
                    tipoDoc = "Factura";
                } else {
                    pdfComprobante = pdfBoletaUtil.generateBoletaPdf(comprobante, request);
                    tipoDoc = "Boleta";
                }

                TicketEmailRequest emailRequest = TicketEmailRequest.builder()
                        .emailDestino(request.getClienteEmail())
                        .usuarioId(request.getClienteDocumento())
                        .nombreUsuario(request.getClienteNombre())
                        .eventoNombre(request.getEventoNombre())
                        .eventoFecha(request.getEventoFecha())
                        .eventoLugar(request.getEventoLugar())
                        .ticketCode(generated.getNumero())
                        .orderReference(request.getTransactionReference())
                        .pdfTicket(pdfComprobante)
                        .build();

                notificationClient.enviarTicketEmail(emailRequest);
                log.info("{} enviada por email a: {}", tipoDoc, request.getClienteEmail());

            } catch (Exception e) {
                log.warn("No se pudo enviar {} por email: {}",
                        request.getTipoComprobante(), e.getMessage());
            }

            return billingResponse;

        } catch (Exception e) {
            log.error("Error en emisión de comprobante: {}", e.getMessage(), e);
            return BillingResponse.builder()
                    .success(false)
                    .sunatCode("-1")
                    .message("Error: " + e.getMessage())
                    .build();
        }
    }
}