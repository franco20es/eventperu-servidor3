package com.example.payment_service.Service.Sunat;

import lombok.extern.slf4j.Slf4j;
import org.apache.xml.security.Init;
import org.apache.xml.security.algorithms.MessageDigestAlgorithm;
import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.transforms.Transforms;
import org.apache.xml.security.utils.Constants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

@Service
@Slf4j
public class XmlSignerService {

    @Value("${sunat.certificado-path}")
    private Resource certificadoPath;

    @Value("${sunat.certificado-password}")
    private String certificadoPassword;

    static {
        Init.init();
    }

    public String signXml(String xmlString) {
        try {

            // 1. Cargar certificado
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(
                    certificadoPath.getInputStream(),
                    certificadoPassword.toCharArray()
            );

            String alias = keyStore.aliases().nextElement();

            PrivateKey privateKey = (PrivateKey) keyStore.getKey(
                    alias,
                    certificadoPassword.toCharArray()
            );

            X509Certificate certificate =
                    (X509Certificate) keyStore.getCertificate(alias);

            // 2. Parse XML
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);

            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(
                    new ByteArrayInputStream(xmlString.getBytes(StandardCharsets.UTF_8))
            );

            // 3. Crear firma (IMPORTANTE SHA256)
            XMLSignature signature = new XMLSignature(
                    doc,
                    "",
                    XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA256
            );

            // 4. Insertar en ExtensionContent
            NodeList extensionContent = doc.getElementsByTagNameNS(
                    "urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2",
                    "ExtensionContent"
            );

            if (extensionContent.getLength() == 0) {
                throw new RuntimeException("No existe ExtensionContent en el XML UBL");
            }

            extensionContent.item(0).appendChild(signature.getElement());

            // 5. Transforms CORRECTOS SUNAT
            Transforms transforms = new Transforms(doc);
            transforms.addTransform(Transforms.TRANSFORM_ENVELOPED_SIGNATURE);
            transforms.addTransform(Transforms.TRANSFORM_C14N_EXCL_OMIT_COMMENTS);

            // 6. Digest SHA256 (IMPORTANTE coherente con SUNAT)
            signature.addDocument(
                    "",
                    transforms,
                    MessageDigestAlgorithm.ALGO_ID_DIGEST_SHA256
            );

            // 7. Certificado
            signature.addKeyInfo(certificate);

            // 8. FIRMA
            signature.sign(privateKey);

            return docToString(doc);

        } catch (Exception e) {
            log.error("Error firmando XML: {}", e.getMessage(), e);
            throw new RuntimeException("Error firmando XML: " + e.getMessage());
        }
    }

    private String docToString(Document doc) throws TransformerException {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();

        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "no");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");

        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));

        return writer.toString();
    }
}