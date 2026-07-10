package com.example.payment_service.Service.Sunat;

import com.example.payment_service.dto.Response.SunatResponse;
import jakarta.xml.soap.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@Slf4j
public class SunatSoapClient {

    @Value("${sunat.url-beta}")
    private String sunatUrl;

    @Value("${sunat.usuario-sol}")
    private String usuarioSol;

    @Value("${sunat.clave-sol}")
    private String claveSol;

    @Value("${sunat.ruc}")
    private String ruc;

    public SunatResponse sendBill(String xmlSigned, String fileName) {
        try {
            // 1. Comprimir XML en ZIP
            byte[] zipBytes = compressToZip(xmlSigned, fileName + ".xml");
            String zipBase64 = Base64.getEncoder().encodeToString(zipBytes);

            // 2. Crear mensaje SOAP
            MessageFactory messageFactory = MessageFactory.newInstance();
            SOAPMessage soapMessage = messageFactory.createMessage();
            SOAPPart soapPart = soapMessage.getSOAPPart();
            SOAPEnvelope envelope = soapPart.getEnvelope();

            // 3. Header WS-Security con usuario/clave SOL
            SOAPHeader header = envelope.getHeader();
            if (header == null) header = envelope.addHeader();

            SOAPElement security = header.addChildElement("Security", "wsse",
                    "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd");

            SOAPElement usernameToken = security.addChildElement("UsernameToken", "wsse");

            SOAPElement usernameEl = usernameToken.addChildElement("Username", "wsse");
            usernameEl.setTextContent(ruc + usuarioSol);

            SOAPElement passwordEl = usernameToken.addChildElement("Password", "wsse");
            passwordEl.setAttribute("Type",
                    "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText");
            passwordEl.setTextContent(claveSol);

            // 4. Body SOAP
            envelope.addNamespaceDeclaration("ser", "http://service.sunat.gob.pe");

            SOAPBody soapBody = envelope.getBody();
            SOAPElement sendBillElement = soapBody.addChildElement("sendBill", "ser");
            sendBillElement.addChildElement("fileName").addTextNode(fileName + ".zip");
            sendBillElement.addChildElement("contentFile").addTextNode(zipBase64);

            soapMessage.saveChanges();

            // 5. Enviar
            SOAPConnectionFactory connectionFactory = SOAPConnectionFactory.newInstance();
            SOAPConnection connection = connectionFactory.createConnection();
            SOAPMessage response = connection.call(soapMessage, sunatUrl);
            connection.close();

            // 6. Procesar respuesta
            return processResponse(response);

        } catch (Exception e) {
            log.error("Error enviando a SUNAT: {}", e.getMessage(), e);
            return SunatResponse.builder()
                    .success(false)
                    .code("-1")
                    .message("Error conectando a SUNAT: " + e.getMessage())
                    .build();
        }
    }

    private SunatResponse processResponse(SOAPMessage response) {
        try {
            SOAPBody responseBody = response.getSOAPBody();

            if (responseBody.hasFault()) {
                SOAPFault fault = responseBody.getFault();
                log.error("SUNAT Fault: {}", fault.getFaultString());
                return SunatResponse.builder()
                        .success(false)
                        .code(fault.getFaultCode())
                        .message(fault.getFaultString())
                        .build();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            response.writeTo(baos);

            log.info("SUNAT respuesta recibida correctamente");

            return SunatResponse.builder()
                    .success(true)
                    .code("0")
                    .message("ACEPTADO")
                    .cdrBase64(Base64.getEncoder().encodeToString(baos.toByteArray()))
                    .build();

        } catch (Exception e) {
            log.error("Error procesando respuesta SUNAT: {}", e.getMessage());
            return SunatResponse.builder()
                    .success(false)
                    .code("-1")
                    .message("Error procesando respuesta: " + e.getMessage())
                    .build();
        }
    }

    private byte[] compressToZip(String content, String entryName) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);
        ZipEntry entry = new ZipEntry(entryName);
        zos.putNextEntry(entry);
        zos.write(content.getBytes("UTF-8"));
        zos.closeEntry();
        zos.close();
        return baos.toByteArray();
    }
}