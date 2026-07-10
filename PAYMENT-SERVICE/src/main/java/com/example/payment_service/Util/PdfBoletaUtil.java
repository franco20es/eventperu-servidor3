package com.example.payment_service.Util;


import com.example.payment_service.Model.ComprobanteModel;
import com.example.payment_service.dto.Request.BillingItemRequest;
import com.example.payment_service.dto.Request.BillingRequest;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@Slf4j
public class PdfBoletaUtil {

    // ─── Colores ───────────────────────────────────────────────────────────────
    private static final BaseColor GREEN      = new BaseColor(0, 200, 83);
    private static final BaseColor GREEN_DARK = new BaseColor(0, 150, 60);
    private static final BaseColor BLACK      = new BaseColor(10, 10, 10);
    private static final BaseColor WHITE      = BaseColor.WHITE;
    private static final BaseColor LIGHT_BG   = new BaseColor(248, 250, 248);
    private static final BaseColor MUTED      = new BaseColor(120, 120, 120);
    private static final BaseColor BORDER     = new BaseColor(220, 230, 222);
    private static final BaseColor TABLE_HDR  = new BaseColor(10, 10, 10);
    private static final BaseColor TABLE_ALT  = new BaseColor(245, 250, 246);

    // ─── Fuentes ───────────────────────────────────────────────────────────────
    private static final Font F_TINY       = new Font(Font.FontFamily.HELVETICA,  6, Font.NORMAL, MUTED);
    private static final Font F_SMALL      = new Font(Font.FontFamily.HELVETICA,  7, Font.NORMAL, MUTED);
    private static final Font F_SMALL_B    = new Font(Font.FontFamily.HELVETICA,  7, Font.BOLD,   BLACK);
    private static final Font F_NORMAL     = new Font(Font.FontFamily.HELVETICA,  9, Font.NORMAL, BLACK);
    private static final Font F_NORMAL_M   = new Font(Font.FontFamily.HELVETICA,  9, Font.NORMAL, MUTED);
    private static final Font F_NORMAL_B   = new Font(Font.FontFamily.HELVETICA,  9, Font.BOLD,   BLACK);
    private static final Font F_NORMAL_G   = new Font(Font.FontFamily.HELVETICA,  9, Font.BOLD,   GREEN_DARK);
    private static final Font F_MEDIUM     = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD,   BLACK);
    private static final Font F_LARGE      = new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD,   BLACK);
    private static final Font F_XLARGE     = new Font(Font.FontFamily.HELVETICA, 28, Font.BOLD,   BLACK);
    private static final Font F_TITLE_G    = new Font(Font.FontFamily.HELVETICA, 28, Font.BOLD,   GREEN_DARK);
    private static final Font F_LABEL      = new Font(Font.FontFamily.HELVETICA,  6, Font.BOLD,   MUTED);
    private static final Font F_TH         = new Font(Font.FontFamily.HELVETICA,  7, Font.BOLD,   WHITE);
    private static final Font F_TD         = new Font(Font.FontFamily.HELVETICA,  8, Font.NORMAL, BLACK);
    private static final Font F_TD_B       = new Font(Font.FontFamily.HELVETICA,  8, Font.BOLD,   BLACK);
    private static final Font F_TOTAL      = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD,   GREEN_DARK);
    private static final Font F_TOTAL_LBL  = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD,   BLACK);
    private static final Font F_FOOTER     = new Font(Font.FontFamily.HELVETICA,  7, Font.NORMAL, MUTED);
    private static final Font F_FOOTER_B   = new Font(Font.FontFamily.HELVETICA,  7, Font.BOLD,   BLACK);

    private static final float PAGE_W = PageSize.A4.getWidth();
    private static final float PAD    = 42;

    public byte[] generateBoletaPdf(ComprobanteModel comprobante, BillingRequest billing) {
        try {
            Document doc = new Document(PageSize.A4, PAD, PAD, PAD, PAD);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            doc.open();

            PdfContentByte cb = writer.getDirectContent();
            float usableW = PAGE_W - PAD * 2;

            // ══════════════════════════════════════════════════════════════════
            // HEADER
            // ══════════════════════════════════════════════════════════════════
            PdfPTable header = new PdfPTable(2);
            header.setWidthPercentage(100);
            header.setWidths(new float[]{1.2f, 1f});
            header.setSpacingAfter(18);

            // ── Izquierda: Logo + datos emisor ────────────────────────────────
            PdfPCell leftCell = new PdfPCell();
            leftCell.setBorder(Rectangle.NO_BORDER);
            leftCell.setPadding(0);

            // Logo desde URL
            try {
                Image logo = Image.getInstance(new URL(
                        "https://res.cloudinary.com/dgrdonnsk/image/upload/v1781050194/svg_eventFast_qnll7a.svg"));
                logo.scaleToFit(120, 45);
                PdfPCell logoCell = new PdfPCell(logo);
                logoCell.setBorder(Rectangle.NO_BORDER);
                logoCell.setPaddingBottom(6);

                PdfPTable logoTable = new PdfPTable(1);
                logoTable.setWidthPercentage(100);
                logoTable.addCell(logoCell);

                addLabelRow(logoTable, "PLATAFORMA DE TICKETS PREMIUM");
                addSpacerRow(logoTable, 6);
                addDataRow(logoTable, "RUC: 20000000001", F_SMALL_B);
                addDataRow(logoTable, "Dir: Av. Javier Prado Este 4200, San Borja, Lima", F_SMALL);
                addDataRow(logoTable, "Tel: +51 01 234-5678  ·  Web: eventperu.com", F_SMALL);

                leftCell.addElement(logoTable);
            } catch (Exception e) {
                // fallback texto si no carga logo
                Paragraph brand = new Paragraph("EventPeru", F_LARGE);
                leftCell.addElement(brand);
            }

            header.addCell(leftCell);

            // ── Derecha: Tipo + número comprobante ────────────────────────────
            PdfPCell rightCell = new PdfPCell();
            rightCell.setBorder(Rectangle.NO_BORDER);
            rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            rightCell.setPadding(0);

            PdfPTable rightTable = new PdfPTable(1);
            rightTable.setWidthPercentage(100);

            // Label tipo
            PdfPCell typeLabel = new PdfPCell(new Phrase(
                    "BOLETA DE VENTA ELECTRÓNICA",
                    new Font(Font.FontFamily.HELVETICA, 7, Font.BOLD,
                            new BaseColor(80, 80, 80))));
            typeLabel.setBorder(Rectangle.NO_BORDER);
            typeLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
            typeLabel.setPaddingBottom(4);
            rightTable.addCell(typeLabel);

            // Número grande
            String[] parts = comprobante.getNumeroComprobante().split("-");
            String serie = parts.length > 0 ? parts[0] : "B001";
            String numero = parts.length > 1
                    ? String.format("%08d", Integer.parseInt(parts[1]))
                    : "00000001";

            PdfPCell numCell = new PdfPCell();
            numCell.setBorder(Rectangle.NO_BORDER);
            numCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            Paragraph numPara = new Paragraph();
            numPara.setAlignment(Element.ALIGN_RIGHT);
            numPara.add(new Chunk(serie + "-", F_XLARGE));
            numPara.add(new Chunk(numero, F_TITLE_G));
            numCell.addElement(numPara);
            rightTable.addCell(numCell);

            // Sub serie
            PdfPCell subSerie = new PdfPCell(new Phrase(
                    "Comprobante electrónico · Serie " + serie, F_SMALL));
            subSerie.setBorder(Rectangle.NO_BORDER);
            subSerie.setHorizontalAlignment(Element.ALIGN_RIGHT);
            subSerie.setPaddingBottom(10);
            rightTable.addCell(subSerie);

            // Fecha
            String fechaStr = comprobante.getFechaEmision() != null
                    ? comprobante.getFechaEmision()
                    .format(DateTimeFormatter.ofPattern("dd 'de' MMMM, yyyy"))
                    : "—";
            String horaStr = comprobante.getFechaEmision() != null
                    ? comprobante.getFechaEmision()
                    .format(DateTimeFormatter.ofPattern("HH:mm:ss")) + " hrs"
                    : "—";

            PdfPCell fechaLbl = new PdfPCell(new Phrase("Fecha de emisión", F_SMALL));
            fechaLbl.setBorder(Rectangle.NO_BORDER);
            fechaLbl.setHorizontalAlignment(Element.ALIGN_RIGHT);
            rightTable.addCell(fechaLbl);

            PdfPCell fechaVal = new PdfPCell(new Phrase(fechaStr,
                    new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BLACK)));
            fechaVal.setBorder(Rectangle.NO_BORDER);
            fechaVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
            rightTable.addCell(fechaVal);

            PdfPCell horaVal = new PdfPCell(new Phrase("Hora: " + horaStr, F_SMALL));
            horaVal.setBorder(Rectangle.NO_BORDER);
            horaVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
            horaVal.setPaddingBottom(8);
            rightTable.addCell(horaVal);

            // Badge SUNAT
            PdfPCell sunatBadge = new PdfPCell();
            sunatBadge.setBorder(Rectangle.BOX);
            sunatBadge.setBorderColor(GREEN_DARK);
            sunatBadge.setBorderWidth(0.8f);
            sunatBadge.setPadding(4);
            sunatBadge.setHorizontalAlignment(Element.ALIGN_RIGHT);
            Paragraph sunatPara = new Paragraph();
            sunatPara.setAlignment(Element.ALIGN_RIGHT);
            sunatPara.add(new Chunk("✓ SUNAT Procesado  ·  ", F_NORMAL_G));
            sunatPara.add(new Chunk("Hash verificado", F_NORMAL_G));
            sunatBadge.addElement(sunatPara);
            rightTable.addCell(sunatBadge);

            rightCell.addElement(rightTable);
            header.addCell(rightCell);
            doc.add(header);

            // Línea divisoria
            addHRule(cb, writer, PAD, doc.top() - doc.topMargin()
                    - header.getTotalHeight() - 4, usableW);

            // ══════════════════════════════════════════════════════════════════
            // DATOS COMPRADOR / VENDEDOR
            // ══════════════════════════════════════════════════════════════════
            PdfPTable parties = new PdfPTable(2);
            parties.setWidthPercentage(100);
            parties.setWidths(new float[]{1f, 1f});
            parties.setSpacingBefore(14);
            parties.setSpacingAfter(14);

            // Comprador
            PdfPCell buyerCell = new PdfPCell();
            buyerCell.setBorder(Rectangle.RIGHT);
            buyerCell.setBorderColor(BORDER);
            buyerCell.setBorderWidthRight(0.5f);
            buyerCell.setPaddingRight(16);
            buyerCell.setPaddingBottom(12);

            Paragraph buyerLbl = new Paragraph("DATOS DEL COMPRADOR", F_LABEL);
            buyerLbl.setSpacingAfter(6);
            buyerCell.addElement(buyerLbl);

            Paragraph buyerName = new Paragraph(
                    billing.getClienteNombre() != null
                            ? billing.getClienteNombre() : comprobante.getClienteNombre(),
                    new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, BLACK));
            buyerName.setSpacingAfter(8);
            buyerCell.addElement(buyerName);

            String tipoDoc = "1".equals(billing.getClienteTipoDoc()) ? "DNI" : "RUC";
            addIconRow(buyerCell, tipoDoc + ": " +
                    (billing.getClienteDocumento() != null
                            ? billing.getClienteDocumento()
                            : comprobante.getClienteDocumento()), F_NORMAL);
            addIconRow(buyerCell, comprobante.getClienteEmail(), F_NORMAL);
            if (billing.getClienteDireccion() != null) {
                addIconRow(buyerCell, billing.getClienteDireccion(), F_NORMAL);
            }

            // Badge DNI/RUC
            PdfPTable docBadge = new PdfPTable(1);
            docBadge.setWidthPercentage(60);
            docBadge.setHorizontalAlignment(Element.ALIGN_LEFT);
            PdfPCell badge = new PdfPCell(new Phrase(
                    tipoDoc + " · " + (billing.getClienteDocumento() != null
                            ? billing.getClienteDocumento()
                            : comprobante.getClienteDocumento()),
                    F_NORMAL_B));
            badge.setBorder(Rectangle.BOX);
            badge.setBorderColor(BORDER);
            badge.setBorderWidth(0.8f);
            badge.setPadding(5);
            badge.setBackgroundColor(LIGHT_BG);
            docBadge.addCell(badge);
            buyerCell.addElement(docBadge);
            parties.addCell(buyerCell);

            // Vendedor
            PdfPCell sellerCell = new PdfPCell();
            sellerCell.setBorder(Rectangle.NO_BORDER);
            sellerCell.setPaddingLeft(16);
            sellerCell.setPaddingBottom(12);

            Paragraph sellerLbl = new Paragraph("DATOS DEL VENDEDOR", F_LABEL);
            sellerLbl.setSpacingAfter(6);
            sellerCell.addElement(sellerLbl);

            Paragraph sellerName = new Paragraph("EventPeru S.A.C.",
                    new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, BLACK));
            sellerName.setSpacingAfter(8);
            sellerCell.addElement(sellerName);

            addIconRow(sellerCell, "RUC: 20000000001", F_NORMAL);
            addIconRow(sellerCell, "Av. Javier Prado Este 4200, San Borja", F_NORMAL);
            addIconRow(sellerCell, "Operador autorizado SUNAT", F_NORMAL);

            // Badge RUC vendedor
            PdfPTable rucBadge = new PdfPTable(1);
            rucBadge.setWidthPercentage(70);
            rucBadge.setHorizontalAlignment(Element.ALIGN_LEFT);
            PdfPCell rucCell = new PdfPCell(new Phrase("RUC · 20000000001", F_NORMAL_B));
            rucCell.setBorder(Rectangle.BOX);
            rucCell.setBorderColor(BORDER);
            rucCell.setBorderWidth(0.8f);
            rucCell.setPadding(5);
            rucCell.setBackgroundColor(LIGHT_BG);
            rucBadge.addCell(rucCell);
            sellerCell.addElement(rucBadge);
            parties.addCell(sellerCell);

            doc.add(parties);

            // ══════════════════════════════════════════════════════════════════
            // EVENTO INFO
            // ══════════════════════════════════════════════════════════════════
            PdfPTable eventBar = new PdfPTable(1);
            eventBar.setWidthPercentage(100);
            eventBar.setSpacingAfter(10);

            PdfPCell eventCell = new PdfPCell();
            eventCell.setBorder(Rectangle.BOTTOM);
            eventCell.setBorderColor(BORDER);
            eventCell.setBorderWidthBottom(0.5f);
            eventCell.setPaddingBottom(8);
            eventCell.setPaddingTop(4);

            Paragraph eventInfo = new Paragraph();
            eventInfo.add(new Chunk("🎟  ", F_NORMAL));
            eventInfo.add(new Chunk(
                    (billing.getEventoNombre() != null
                            ? billing.getEventoNombre().toUpperCase()
                            : comprobante.getEventoNombre().toUpperCase()) + "   ·   ",
                    new Font(Font.FontFamily.HELVETICA, 7, Font.BOLD,
                            new BaseColor(80, 80, 80))));

            Paragraph eventName = new Paragraph(
                    billing.getEventoNombre() != null
                            ? billing.getEventoNombre()
                            : comprobante.getEventoNombre(),
                    new Font(Font.FontFamily.HELVETICA, 15, Font.BOLD,
                            new BaseColor(160, 160, 160)));
            eventName.setSpacingBefore(2);

            Paragraph eventMeta = new Paragraph();
            if (billing.getEventoFecha() != null) {
                eventMeta.add(new Chunk(billing.getEventoFecha() + "   ",
                        F_SMALL_B));
            }
            if (billing.getEventoLugar() != null) {
                eventMeta.add(new Chunk(billing.getEventoLugar(), F_SMALL));
            }
            eventMeta.setSpacingBefore(2);

            eventCell.addElement(eventInfo);
            eventCell.addElement(eventName);
            eventCell.addElement(eventMeta);
            eventBar.addCell(eventCell);
            doc.add(eventBar);

            // ══════════════════════════════════════════════════════════════════
            // TABLA DE ITEMS
            // ══════════════════════════════════════════════════════════════════
            PdfPTable itemsTable = new PdfPTable(5);
            itemsTable.setWidthPercentage(100);
            itemsTable.setWidths(new float[]{0.4f, 2.8f, 0.7f, 0.9f, 0.9f});
            itemsTable.setSpacingAfter(0);

            // Headers
            String[] headers = {"#", "DESCRIPCIÓN DEL PRODUCTO / SERVICIO",
                    "CANT.", "P. UNIT.", "SUBTOTAL"};
            for (String h : headers) {
                PdfPCell thCell = new PdfPCell(new Phrase(h, F_TH));
                thCell.setBackgroundColor(TABLE_HDR);
                thCell.setPadding(6);
                thCell.setBorder(Rectangle.NO_BORDER);
                thCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                itemsTable.addCell(thCell);
            }

            // Items
            List<BillingItemRequest> items = billing.getItems();
            if (items != null && !items.isEmpty()) {
                int idx = 1;
                double subtotalTotal = 0;
                for (BillingItemRequest item : items) {
                    BaseColor rowBg = (idx % 2 == 0) ? TABLE_ALT : WHITE;

                    addItemCell(itemsTable, String.format("0%d", idx),
                            Element.ALIGN_CENTER, F_TD, rowBg, true);

                    // Descripción con detalle
                    PdfPCell descCell = new PdfPCell();
                    descCell.setBackgroundColor(rowBg);
                    descCell.setBorder(Rectangle.NO_BORDER);
                    descCell.setPadding(6);
                    Paragraph descP = new Paragraph(
                            item.getDescripcion() != null ? item.getDescripcion() : "—",
                            F_TD_B);
                    descP.setSpacingAfter(2);
                    String detalle = "";
                    if (billing.getEventoNombre() != null)
                        detalle += billing.getEventoNombre();
                    if (billing.getEventoFecha() != null)
                        detalle += " · " + billing.getEventoFecha();
                    if (billing.getEventoLugar() != null)
                        detalle += " · " + billing.getEventoLugar();
                    if (!detalle.isEmpty()) {
                        Paragraph detalleP = new Paragraph(detalle,
                                new Font(Font.FontFamily.HELVETICA, 7,
                                        Font.NORMAL, MUTED));
                        descCell.addElement(descP);
                        descCell.addElement(detalleP);
                    } else {
                        descCell.addElement(descP);
                    }
                    itemsTable.addCell(descCell);

                    int cant = item.getCantidad();
                    double pu = item.getPrecioUnitario();
                    double sub = cant * pu;
                    subtotalTotal += sub;

                    addItemCell(itemsTable,
                            String.valueOf(cant), Element.ALIGN_CENTER, F_TD, rowBg, false);
                    addItemCell(itemsTable,
                            String.format("S/ %.2f", pu),
                            Element.ALIGN_RIGHT, F_TD, rowBg, false);
                    addItemCell(itemsTable,
                            String.format("S/ %.2f", sub),
                            Element.ALIGN_RIGHT, F_TD_B, rowBg, false);
                    idx++;
                }

                // ── TOTALES ────────────────────────────────────────────────
                double igv = subtotalTotal * 0.18 / 1.18;
                double sinIgv = subtotalTotal - igv;
                double total = comprobante.getTotalVenta() != null
                        ? comprobante.getTotalVenta() : subtotalTotal;

                doc.add(itemsTable);

                // Tabla totales
                PdfPTable totals = new PdfPTable(2);
                totals.setWidthPercentage(45);
                totals.setHorizontalAlignment(Element.ALIGN_RIGHT);
                totals.setWidths(new float[]{1.4f, 1f});
                totals.setSpacingBefore(0);
                totals.setSpacingAfter(16);

                addTotalRow(totals, "Subtotal (sin IGV)",
                        String.format("S/ %.2f", sinIgv), F_NORMAL, F_NORMAL, WHITE);
                addTotalRow(totals, "IGV (18%)  ✦ Incluido",
                        String.format("S/ %.2f", igv), F_NORMAL_G, F_NORMAL_G,
                        new BaseColor(240, 255, 245));
                addTotalRow(totals, "Descuentos",
                        "S/ 0.00", F_NORMAL, F_NORMAL, WHITE);

                // Fila TOTAL
                PdfPCell totalLblCell = new PdfPCell(
                        new Phrase("TOTAL A PAGAR", F_TOTAL_LBL));
                totalLblCell.setBorder(Rectangle.TOP);
                totalLblCell.setBorderColor(BLACK);
                totalLblCell.setBorderWidthTop(1.5f);
                totalLblCell.setPadding(8);
                totals.addCell(totalLblCell);

                PdfPCell totalValCell = new PdfPCell(
                        new Phrase(String.format("S/ %.2f", total), F_TOTAL));
                totalValCell.setBorder(Rectangle.TOP);
                totalValCell.setBorderColor(BLACK);
                totalValCell.setBorderWidthTop(1.5f);
                totalValCell.setPadding(8);
                totalValCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                totals.addCell(totalValCell);

                doc.add(totals);
            } else {
                doc.add(itemsTable);
            }

            // ══════════════════════════════════════════════════════════════════
            // REFERENCIA DE TRANSACCIÓN
            // ══════════════════════════════════════════════════════════════════
            PdfPTable txnTable = new PdfPTable(1);
            txnTable.setWidthPercentage(100);
            txnTable.setSpacingAfter(16);

            PdfPCell txnCell = new PdfPCell();
            txnCell.setBorder(Rectangle.BOX);
            txnCell.setBorderColor(BORDER);
            txnCell.setBorderWidth(0.5f);
            txnCell.setBackgroundColor(LIGHT_BG);
            txnCell.setPadding(10);

            Paragraph txnLbl = new Paragraph("REFERENCIA DE TRANSACCIÓN", F_LABEL);
            txnLbl.setSpacingAfter(6);
            txnCell.addElement(txnLbl);

            Paragraph txnRef = new Paragraph();
            txnRef.add(new Chunk("Referencia:  ", F_SMALL));
            txnRef.add(new Chunk(comprobante.getTransactionReference(), F_SMALL_B));
            txnRef.setSpacingAfter(3);
            txnCell.addElement(txnRef);

            Paragraph txnEmail = new Paragraph();
            txnEmail.add(new Chunk("Tickets enviados a:  ", F_SMALL));
            txnEmail.add(new Chunk(comprobante.getClienteEmail(), F_SMALL_B));
            txnCell.addElement(txnEmail);

            txnTable.addCell(txnCell);
            doc.add(txnTable);

            // ══════════════════════════════════════════════════════════════════
            // FOOTER
            // ══════════════════════════════════════════════════════════════════
            PdfPTable footer = new PdfPTable(2);
            footer.setWidthPercentage(100);
            footer.setWidths(new float[]{1.8f, 1f});

            PdfPCell footerLeft = new PdfPCell();
            footerLeft.setBorder(Rectangle.NO_BORDER);
            Paragraph footerNote = new Paragraph(
                    "Representación impresa de la ", F_FOOTER);
            footerNote.add(new Chunk("Boleta de Venta Electrónica.", F_FOOTER_B));
            footerNote.add(new Chunk(
                    " Puede consultar la validez de este documento en ",
                    F_FOOTER));
            footerNote.add(new Chunk("sunat.gob.pe", F_FOOTER_B));
            footerNote.add(new Chunk(
                    " con la serie y número indicados. Para soporte: ",
                    F_FOOTER));
            footerNote.add(new Chunk("soporte@eventperu.com", F_FOOTER_B));
            footerLeft.addElement(footerNote);
            footer.addCell(footerLeft);

            PdfPCell footerRight = new PdfPCell();
            footerRight.setBorder(Rectangle.NO_BORDER);
            footerRight.setHorizontalAlignment(Element.ALIGN_RIGHT);
            Paragraph hashPara = new Paragraph(
                    "Hash: " + comprobante.getSunatMensaje()
                            .substring(0, Math.min(20, comprobante.getSunatMensaje().length()))
                            + "...",
                    F_FOOTER);
            hashPara.setAlignment(Element.ALIGN_RIGHT);
            Paragraph emisorPara = new Paragraph(
                    "Emitido por EventPeru · Sistema FE v2.4", F_FOOTER);
            emisorPara.setAlignment(Element.ALIGN_RIGHT);
            footerRight.addElement(hashPara);
            footerRight.addElement(emisorPara);
            footer.addCell(footerRight);

            doc.add(footer);
            doc.close();

            log.info("PDF boleta generado: {}", comprobante.getNumeroComprobante());
            return out.toByteArray();

        } catch (Exception e) {
            log.error("Error generando PDF boleta: {}", e.getMessage());
            throw new RuntimeException("Error generando boleta PDF", e);
        }
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────
    private void addHRule(PdfContentByte cb, PdfWriter writer,
                          float x, float y, float w) {
        cb.saveState();
        cb.setColorStroke(BORDER);
        cb.setLineWidth(0.5f);
        cb.moveTo(x, y);
        cb.lineTo(x + w, y);
        cb.stroke();
        cb.restoreState();
    }

    private void addLabelRow(PdfPTable t, String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, F_LABEL));
        c.setBorder(Rectangle.NO_BORDER);
        c.setPaddingBottom(3);
        t.addCell(c);
    }

    private void addSpacerRow(PdfPTable t, float h) {
        PdfPCell c = new PdfPCell(new Phrase(" "));
        c.setBorder(Rectangle.NO_BORDER);
        c.setFixedHeight(h);
        t.addCell(c);
    }

    private void addDataRow(PdfPTable t, String text, Font f) {
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setBorder(Rectangle.NO_BORDER);
        c.setPaddingBottom(2);
        t.addCell(c);
    }

    private void addIconRow(PdfPCell cell, String text, Font f) {
        Paragraph p = new Paragraph(text, f);
        p.setSpacingAfter(3);
        cell.addElement(p);
    }

    private void addItemCell(PdfPTable t, String text, int align,
                             Font f, BaseColor bg, boolean first) {
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setBackgroundColor(bg);
        c.setBorder(Rectangle.NO_BORDER);
        c.setPadding(6);
        c.setHorizontalAlignment(align);
        t.addCell(c);
    }

    private void addTotalRow(PdfPTable t, String lbl, String val,
                             Font lf, Font vf, BaseColor bg) {
        PdfPCell lc = new PdfPCell(new Phrase(lbl, lf));
        lc.setBorder(Rectangle.NO_BORDER);
        lc.setBackgroundColor(bg);
        lc.setPadding(5);
        t.addCell(lc);

        PdfPCell vc = new PdfPCell(new Phrase(val, vf));
        vc.setBorder(Rectangle.NO_BORDER);
        vc.setBackgroundColor(bg);
        vc.setPadding(5);
        vc.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(vc);
    }
}