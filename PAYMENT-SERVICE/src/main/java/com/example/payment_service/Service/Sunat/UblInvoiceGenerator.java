package com.example.payment_service.Service.Sunat;

import com.example.payment_service.Repository.ComprobanteRepository;
import com.example.payment_service.dto.Request.BillingRequest;
import com.example.payment_service.dto.Request.BillingItemRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
@Slf4j
public class UblInvoiceGenerator {

    private static final String CBC_NS = "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2";
    private static final String CAC_NS = "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2";
    private static final String EXT_NS = "urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2";

    private final ComprobanteRepository comprobanteRepository;

    public UblInvoiceGenerator(ComprobanteRepository comprobanteRepository) {
        this.comprobanteRepository = comprobanteRepository;
    }

    public GeneratedInvoice generateXml(BillingRequest request, String ruc, String razonSocial) {
        try {
            boolean esBoleta = "boleta".equalsIgnoreCase(request.getTipoComprobante());
            String tipoDoc = esBoleta ? "03" : "01";
            String serie   = esBoleta ? "B001" : "F001";

            // Correlativo desde BD
            int correlativo = comprobanteRepository
                    .findTopByTipoComprobanteOrderByFechaEmisionDesc(request.getTipoComprobante())
                    .map(c -> Integer.parseInt(c.getNumeroComprobante().split("-")[1]) + 1)
                    .orElse(1);

            String numero = serie + "-" + correlativo;

            // Totales
            double totalGravado = 0;
            for (BillingItemRequest item : request.getItems()) {
                totalGravado += (item.getPrecioUnitario() / 1.18) * item.getCantidad();
            }
            double igv        = round(totalGravado * 0.18);
            totalGravado      = round(totalGravado);
            double totalVenta = round(totalGravado + igv);

            // DOM
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            Document doc = dbf.newDocumentBuilder().newDocument();

            Element invoice = doc.createElementNS(
                    "urn:oasis:names:specification:ubl:schema:xsd:Invoice-2", "Invoice");
            invoice.setAttribute("xmlns",     "urn:oasis:names:specification:ubl:schema:xsd:Invoice-2");
            invoice.setAttribute("xmlns:cac", CAC_NS);
            invoice.setAttribute("xmlns:cbc", CBC_NS);
            invoice.setAttribute("xmlns:ext", EXT_NS);
            doc.appendChild(invoice);

            // UBLExtensions
            Element ublExts  = doc.createElementNS(EXT_NS, "ext:UBLExtensions");
            Element ublExt   = doc.createElementNS(EXT_NS, "ext:UBLExtension");
            Element extCont  = doc.createElementNS(EXT_NS, "ext:ExtensionContent");
            ublExt.appendChild(extCont);
            ublExts.appendChild(ublExt);
            invoice.appendChild(ublExts);

            // Cabecera
            addElement(doc, invoice, "cbc:UBLVersionID",    "2.1");
            addElement(doc, invoice, "cbc:CustomizationID", "2.0");
            addElement(doc, invoice, "cbc:ProfileID",       "0101");
            addElement(doc, invoice, "cbc:ID",              numero);
            addElement(doc, invoice, "cbc:IssueDate",
                    LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

            Element invTypeCode = doc.createElementNS(CBC_NS, "cbc:InvoiceTypeCode");
            invTypeCode.setAttribute("listID", "0101");
            invTypeCode.setTextContent(tipoDoc);
            invoice.appendChild(invTypeCode);

            Element currCode = doc.createElementNS(CBC_NS, "cbc:DocumentCurrencyCode");
            currCode.setAttribute("listID", "ISO 4217 Alpha");
            currCode.setTextContent("PEN");
            invoice.appendChild(currCode);

            // Emisor
//                Element supplier = doc.createElementNS(CAC_NS, "cac:AccountingSupplierParty");
//                Element supParty = doc.createElementNS(CAC_NS, "cac:Party");
//                Element supPartyId = doc.createElementNS(CAC_NS, "cac:PartyIdentification");
//                Element supId = doc.createElementNS(CBC_NS, "cbc:ID");
//                supId.setAttribute("schemeID", "6");
//                supId.setTextContent(ruc);
//                supPartyId.appendChild(supId);
//                supParty.appendChild(supPartyId);
//            Element supLegal = doc.createElementNS(CAC_NS, "cac:PartyLegalEntity");
//            addElement(doc, supLegal, "cbc:RegistrationName", razonSocial);
//            supParty.appendChild(supLegal);
//            supplier.appendChild(supParty);
//            invoice.appendChild(supplier);

//            Element supLegal = doc.createElementNS(CAC_NS, "cac:PartyLegalEntity");
//            addElement(doc, supLegal, "cbc:RegistrationName", razonSocial);
//            supParty.appendChild(supLegal);
//
//            supplier.appendChild(supParty);
//            invoice.appendChild(supplier);
            // Emisor
            Element supplier = doc.createElementNS(CAC_NS, "cac:AccountingSupplierParty");
            Element supParty = doc.createElementNS(CAC_NS, "cac:Party");

// RUC del emisor
            Element supPartyId = doc.createElementNS(CAC_NS, "cac:PartyIdentification");
            Element supId = doc.createElementNS(CBC_NS, "cbc:ID");
            supId.setAttribute("schemeID", "6");
            supId.setTextContent(ruc);
            supPartyId.appendChild(supId);
            supParty.appendChild(supPartyId);

// PostalAddress — requerido por SUNAT (error 3030)
            Element supAddress = doc.createElementNS(CAC_NS, "cac:PostalAddress");
            addElement(doc, supAddress, "cbc:ID", "0000");
            addElement(doc, supAddress, "cbc:StreetName", "Av. Javier Prado Este 4200");
            addElement(doc, supAddress, "cbc:CitySubdivisionName", "SAN BORJA");
            addElement(doc, supAddress, "cbc:CityName", "LIMA");
            addElement(doc, supAddress, "cbc:CountrySubentity", "LIMA");
            Element supCountry = doc.createElementNS(CAC_NS, "cac:Country");
            addElement(doc, supCountry, "cbc:IdentificationCode", "PE");
            supAddress.appendChild(supCountry);
            supParty.appendChild(supAddress);

// Razón social del emisor
            Element supLegal = doc.createElementNS(CAC_NS, "cac:PartyLegalEntity");
            addElement(doc, supLegal, "cbc:RegistrationName", razonSocial);
            supParty.appendChild(supLegal);

            supplier.appendChild(supParty);
            invoice.appendChild(supplier);

            // Cliente
            Element customer = doc.createElementNS(CAC_NS, "cac:AccountingCustomerParty");
            Element cusParty = doc.createElementNS(CAC_NS, "cac:Party");
            Element cusPartyId = doc.createElementNS(CAC_NS, "cac:PartyIdentification");
            Element cusId = doc.createElementNS(CBC_NS, "cbc:ID");
            cusId.setAttribute("schemeID", request.getClienteTipoDoc());
            cusId.setTextContent(request.getClienteDocumento());
            cusPartyId.appendChild(cusId);
            cusParty.appendChild(cusPartyId);
            Element cusLegal = doc.createElementNS(CAC_NS, "cac:PartyLegalEntity");
            addElement(doc, cusLegal, "cbc:RegistrationName", request.getClienteNombre());
            cusParty.appendChild(cusLegal);
            customer.appendChild(cusParty);
            invoice.appendChild(customer);

            // TaxTotal global
            Element taxTotal = doc.createElementNS(CAC_NS, "cac:TaxTotal");
            addAmountElement(doc, taxTotal, "cbc:TaxAmount", String.valueOf(igv));
            Element taxSub = doc.createElementNS(CAC_NS, "cac:TaxSubtotal");
            addAmountElement(doc, taxSub, "cbc:TaxableAmount", String.valueOf(totalGravado));
            addAmountElement(doc, taxSub, "cbc:TaxAmount",     String.valueOf(igv));
            Element taxCat = doc.createElementNS(CAC_NS, "cac:TaxCategory");
            Element taxSch = doc.createElementNS(CAC_NS, "cac:TaxScheme");
            addElement(doc, taxSch, "cbc:ID",          "1000");
            addElement(doc, taxSch, "cbc:Name",        "IGV");
            addElement(doc, taxSch, "cbc:TaxTypeCode", "VAT");
            taxCat.appendChild(taxSch);
            taxSub.appendChild(taxCat);
            taxTotal.appendChild(taxSub);
            invoice.appendChild(taxTotal);

            // LegalMonetaryTotal
            Element lmt = doc.createElementNS(CAC_NS, "cac:LegalMonetaryTotal");
            addAmountElement(doc, lmt, "cbc:LineExtensionAmount", String.valueOf(totalGravado));
            addAmountElement(doc, lmt, "cbc:TaxExclusiveAmount",  String.valueOf(totalGravado));
            addAmountElement(doc, lmt, "cbc:TaxInclusiveAmount",  String.valueOf(totalVenta));
            addAmountElement(doc, lmt, "cbc:PayableAmount",       String.valueOf(totalVenta));
            invoice.appendChild(lmt);

            // ── InvoiceLines ──────────────────────────────────────────────────
            int lineNum = 1;
            for (BillingItemRequest item : request.getItems()) {
                double valorUnitario = round(item.getPrecioUnitario() / 1.18);
                double valorVenta    = round(valorUnitario * item.getCantidad());
                double igvItem       = round(valorVenta * 0.18);

                Element line = doc.createElementNS(CAC_NS, "cac:InvoiceLine");
                addElement(doc, line, "cbc:ID", String.valueOf(lineNum++));

                Element qty = doc.createElementNS(CBC_NS, "cbc:InvoicedQuantity");
                qty.setAttribute("unitCode", "ZZ");
                qty.setTextContent(String.valueOf(item.getCantidad()));
                line.appendChild(qty);

                addAmountElement(doc, line, "cbc:LineExtensionAmount",
                        String.valueOf(valorVenta));

                // ── FIX: cac:PricingReference → AlternativeConditionPrice ────
                // SUNAT error 2028: este tag es obligatorio en boletas y facturas
                Element pricingRef = doc.createElementNS(CAC_NS, "cac:PricingReference");
                Element altCondPrice = doc.createElementNS(CAC_NS, "cac:AlternativeConditionPrice");

                // Precio unitario CON IGV (precio al público)
                Element altPrice = doc.createElementNS(CBC_NS, "cbc:PriceAmount");
                altPrice.setAttribute("currencyID", "PEN");
                altPrice.setTextContent(String.valueOf(round(item.getPrecioUnitario())));
                altCondPrice.appendChild(altPrice);

                // 01 = precio unitario con IGV
                Element priceTypeCode = doc.createElementNS(CBC_NS, "cbc:PriceTypeCode");
                priceTypeCode.setTextContent("01");
                altCondPrice.appendChild(priceTypeCode);

                pricingRef.appendChild(altCondPrice);
                line.appendChild(pricingRef);
                // ────────────────────────────────────────────────────────────────

                // TaxTotal del item (con TaxSubtotal — también requerido)
                Element itemTaxTotal = doc.createElementNS(CAC_NS, "cac:TaxTotal");
                addAmountElement(doc, itemTaxTotal, "cbc:TaxAmount", String.valueOf(igvItem));
                Element itemTaxSub = doc.createElementNS(CAC_NS, "cac:TaxSubtotal");
                addAmountElement(doc, itemTaxSub, "cbc:TaxableAmount", String.valueOf(valorVenta));
                addAmountElement(doc, itemTaxSub, "cbc:TaxAmount",     String.valueOf(igvItem));
                Element itemTaxCat = doc.createElementNS(CAC_NS, "cac:TaxCategory");
                // "10" = Gravado - Operación Onerosa (requerido por SUNAT error 2371)
                addElement(doc, itemTaxCat, "cbc:Percent", "18");
                addElement(doc, itemTaxCat, "cbc:TaxExemptionReasonCode", "10");
                Element itemTaxSch = doc.createElementNS(CAC_NS, "cac:TaxScheme");
                addElement(doc, itemTaxSch, "cbc:ID",          "1000");
                addElement(doc, itemTaxSch, "cbc:Name",        "IGV");
                addElement(doc, itemTaxSch, "cbc:TaxTypeCode", "VAT");
                itemTaxCat.appendChild(itemTaxSch);
                itemTaxSub.appendChild(itemTaxCat);
                itemTaxTotal.appendChild(itemTaxSub);
                line.appendChild(itemTaxTotal);

                Element itemEl = doc.createElementNS(CAC_NS, "cac:Item");
                addElement(doc, itemEl, "cbc:Description",
                        item.getDescripcion() + " - Zona: " + item.getZona());
                line.appendChild(itemEl);

                // Precio SIN IGV (base imponible unitaria)
                Element price = doc.createElementNS(CAC_NS, "cac:Price");
                addAmountElement(doc, price, "cbc:PriceAmount", String.valueOf(valorUnitario));
                line.appendChild(price);

                invoice.appendChild(line);
            }

            return GeneratedInvoice.builder()
                    .xml(docToString(doc))
                    .numero(numero)
                    .serie(serie)
                    .correlativo(String.valueOf(correlativo))
                    .tipoDoc(tipoDoc)
                    .totalVenta(totalVenta)
                    .build();

        } catch (Exception e) {
            log.error("Error generando XML UBL: {}", e.getMessage(), e);
            throw new RuntimeException("Error generando XML: " + e.getMessage());
        }
    }

    private void addElement(Document doc, Element parent, String tag, String value) {
        String ns = tag.startsWith("cbc:") ? CBC_NS : CAC_NS;
        Element el = doc.createElementNS(ns, tag);
        el.setTextContent(value);
        parent.appendChild(el);
    }

    private void addAmountElement(Document doc, Element parent, String tag, String value) {
        Element el = doc.createElementNS(CBC_NS, tag);
        el.setAttribute("currencyID", "PEN");
        el.setTextContent(value);
        parent.appendChild(el);
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private String docToString(Document doc) throws TransformerException {
        Transformer tf = TransformerFactory.newInstance().newTransformer();
        tf.setOutputProperty(OutputKeys.INDENT, "yes");
        tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        StringWriter writer = new StringWriter();
        tf.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.getBuffer().toString();
    }
}