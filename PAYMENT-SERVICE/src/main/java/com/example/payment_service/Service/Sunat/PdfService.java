package com.example.payment_service.Service.Sunat;

import com.example.payment_service.dto.Request.BillingRequest;
import com.example.payment_service.dto.Response.BillingResponse;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

@Service
@Slf4j
public class PdfService {

    private static final BaseColor AZUL = new BaseColor(21, 101, 192);
    private static final BaseColor AZUL_OSCURO = new BaseColor(26, 26, 46);
    private static final BaseColor GRIS_CLARO = new BaseColor(245, 248, 254);
    private static final BaseColor BLANCO = BaseColor.WHITE;

    public String generarPdfBase64(BillingRequest request, BillingResponse billingResponse) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 40, 40, 40, 40);
            PdfWriter.getInstance(document, baos);
            document.open();
            agregarContenido(document, request, billingResponse);
            document.close();
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            log.error("Error generando PDF: {}", e.getMessage());
            return null;
        }
    }

    private void agregarContenido(Document doc, BillingRequest req,
                                  BillingResponse res) throws DocumentException {
        Font fontTitulo = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD, BLANCO);
        Font fontSubtitulo = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, new BaseColor(187, 222, 251));
        Font fontNormal = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, AZUL_OSCURO);
        Font fontBold = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, AZUL_OSCURO);
        Font fontSmall = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, new BaseColor(100, 100, 100));
        Font fontWhiteBold = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, BLANCO);

        // HEADER
        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{60, 40});

        PdfPCell logoCell = new PdfPCell();
        logoCell.setBackgroundColor(AZUL);
        logoCell.setPadding(16);
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.addElement(new Paragraph("EventPeruService", fontTitulo));
        logoCell.addElement(new Paragraph("Plataforma de venta de entradas", fontSubtitulo));
        logoCell.addElement(new Paragraph("www.eventperuservice.com", fontSubtitulo));
        header.addCell(logoCell);

        boolean esBoleta = "boleta".equalsIgnoreCase(req.getTipoComprobante());
        PdfPCell docCell = new PdfPCell();
        docCell.setBackgroundColor(AZUL);
        docCell.setPadding(16);
        docCell.setBorder(Rectangle.NO_BORDER);
        docCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        Font fontDocLabel = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, new BaseColor(187, 222, 251));
        docCell.addElement(new Paragraph(
                esBoleta ? "BOLETA DE VENTA ELECTRONICA" : "FACTURA ELECTRONICA",
                fontDocLabel));
        Font fontDocNum = new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD, BLANCO);
        docCell.addElement(new Paragraph(
                res.getNumeroComprobante() != null ? res.getNumeroComprobante() : "-",
                fontDocNum));
        header.addCell(docCell);
        doc.add(header);

        // RUC BAND
        PdfPTable rucBand = new PdfPTable(1);
        rucBand.setWidthPercentage(100);
        PdfPCell rucCell = new PdfPCell(
                new Phrase("R.U.C. 20000000001  -  EVENTPERU SERVICE S.A.C.", fontSmall));
        rucCell.setBackgroundColor(GRIS_CLARO);
        rucCell.setPadding(6);
        rucCell.setBorder(Rectangle.BOTTOM);
        rucCell.setBorderColor(new BaseColor(220, 227, 239));
        rucBand.addCell(rucCell);
        doc.add(rucBand);

        doc.add(Chunk.NEWLINE);

        // INFO GRID
        PdfPTable infoGrid = new PdfPTable(2);
        infoGrid.setWidthPercentage(100);
        infoGrid.setSpacingAfter(10);

        PdfPCell clienteTitle = new PdfPCell(
                new Phrase("DATOS DEL CLIENTE",
                        new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, BLANCO)));
        clienteTitle.setBackgroundColor(AZUL);
        clienteTitle.setPadding(5);
        clienteTitle.setBorder(Rectangle.NO_BORDER);

        PdfPCell eventoTitle = new PdfPCell(
                new Phrase("DATOS DEL EVENTO",
                        new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, BLANCO)));
        eventoTitle.setBackgroundColor(AZUL);
        eventoTitle.setPadding(5);
        eventoTitle.setBorder(Rectangle.NO_BORDER);

        infoGrid.addCell(clienteTitle);
        infoGrid.addCell(eventoTitle);

        PdfPCell clienteData = new PdfPCell();
        clienteData.setPadding(8);
        clienteData.setBorder(Rectangle.BOX);
        clienteData.setBorderColor(new BaseColor(220, 227, 239));
        clienteData.addElement(new Paragraph("Nombre: " + req.getClienteNombre(), fontNormal));
        clienteData.addElement(new Paragraph("Doc.: " + req.getClienteDocumento(), fontNormal));
        clienteData.addElement(new Paragraph("Email: " + req.getClienteEmail(), fontNormal));
        infoGrid.addCell(clienteData);

        PdfPCell eventoData = new PdfPCell();
        eventoData.setPadding(8);
        eventoData.setBorder(Rectangle.BOX);
        eventoData.setBorderColor(new BaseColor(220, 227, 239));
        eventoData.addElement(new Paragraph("Evento: " + req.getEventoNombre(), fontNormal));
        eventoData.addElement(new Paragraph("Fecha: " + req.getEventoFecha(), fontNormal));
        eventoData.addElement(new Paragraph("Lugar: " + req.getEventoLugar(), fontNormal));
        infoGrid.addCell(eventoData);

        doc.add(infoGrid);

        // TABLA ENTRADAS
        PdfPTable tabla = new PdfPTable(5);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{10, 35, 20, 17, 18});
        tabla.setSpacingAfter(10);

        String[] headers = {"CANT.", "DESCRIPCION", "ZONA", "P. UNIT.", "TOTAL"};
        for (String h : headers) {
            PdfPCell hCell = new PdfPCell(
                    new Phrase(h, new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, BLANCO)));
            hCell.setBackgroundColor(AZUL_OSCURO);
            hCell.setPadding(7);
            hCell.setBorder(Rectangle.NO_BORDER);
            tabla.addCell(hCell);
        }

        double totalGravado = 0;
        for (var item : req.getItems()) {
            double totalItem = item.getPrecioUnitario() * item.getCantidad();
            totalGravado += totalItem / 1.18;

            addTableCell(tabla, String.valueOf(item.getCantidad()), fontNormal, Element.ALIGN_CENTER);
            addTableCell(tabla, item.getDescripcion(), fontNormal, Element.ALIGN_LEFT);
            addTableCell(tabla, item.getZona(), fontNormal, Element.ALIGN_CENTER);
            addTableCell(tabla, "S/ " + String.format("%.2f", item.getPrecioUnitario()),
                    fontNormal, Element.ALIGN_RIGHT);
            addTableCell(tabla, "S/ " + String.format("%.2f", totalItem),
                    fontBold, Element.ALIGN_RIGHT);
        }
        doc.add(tabla);

        // TOTALES
        double igv = totalGravado * 0.18;
        double total = totalGravado + igv;

        PdfPTable totales = new PdfPTable(2);
        totales.setWidthPercentage(40);
        totales.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totales.setSpacingAfter(10);

        addTotalRow(totales, "Op. Gravadas", "S/ " + String.format("%.2f", totalGravado),
                fontNormal, false);
        addTotalRow(totales, "IGV (18%)", "S/ " + String.format("%.2f", igv),
                fontNormal, false);
        addTotalRow(totales, "TOTAL A PAGAR", "S/ " + String.format("%.2f", total),
                fontWhiteBold, true);
        doc.add(totales);

        // ESTADO
        PdfPTable estado = new PdfPTable(1);
        estado.setWidthPercentage(100);
        PdfPCell estadoCell = new PdfPCell(
                new Phrase("COMPROBANTE " + (res.isSuccess() ? "ACEPTADO POR SUNAT" : "PENDIENTE"),
                        new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, BLANCO)));
        estadoCell.setBackgroundColor(
                res.isSuccess() ? new BaseColor(27, 94, 32) : new BaseColor(230, 81, 0));
        estadoCell.setPadding(7);
        estadoCell.setBorder(Rectangle.NO_BORDER);
        estadoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        estado.addCell(estadoCell);
        doc.add(estado);

        // FOOTER
        doc.add(Chunk.NEWLINE);
        Paragraph footer = new Paragraph(
                "Representacion impresa de " + (esBoleta ? "Boleta" : "Factura") +
                        " Electronica. Ref: " +
                        (res.getNumeroComprobante() != null ? res.getNumeroComprobante() : "-") +
                        "  |  EventPeruService - www.eventperuservice.com",
                fontSmall);
        footer.setAlignment(Element.ALIGN_CENTER);
        doc.add(footer);
    }

    private void addTableCell(PdfPTable table, String text, Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(7);
        cell.setHorizontalAlignment(align);
        cell.setBorderColor(new BaseColor(220, 227, 239));
        table.addCell(cell);
    }

    private void addTotalRow(PdfPTable table, String label, String value,
                             Font font, boolean highlight) {
        BaseColor bg = highlight ? AZUL : BLANCO;
        Font labelFont = highlight ? font :
                new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, new BaseColor(100, 100, 100));

        PdfPCell lCell = new PdfPCell(new Phrase(label, labelFont));
        lCell.setBackgroundColor(bg);
        lCell.setPadding(6);
        lCell.setBorderColor(new BaseColor(220, 227, 239));
        table.addCell(lCell);

        PdfPCell vCell = new PdfPCell(new Phrase(value, font));
        vCell.setBackgroundColor(bg);
        vCell.setPadding(6);
        vCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        vCell.setBorderColor(new BaseColor(220, 227, 239));
        table.addCell(vCell);
    }
}
