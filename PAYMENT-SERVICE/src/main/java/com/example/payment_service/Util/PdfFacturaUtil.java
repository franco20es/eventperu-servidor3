package com.example.payment_service.Util;

import com.example.payment_service.Model.ComprobanteModel;
import com.example.payment_service.dto.Request.BillingItemRequest;
import com.example.payment_service.dto.Request.BillingRequest;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@Slf4j
public class PdfFacturaUtil {

    // ─── Colores ───────────────────────────────────────────────────────────────
    private static final BaseColor BLUE      = new BaseColor(26, 42, 94);   // azul oscuro EventPeru
    private static final BaseColor BLUE_MED  = new BaseColor(42, 99, 230);  // azul medio
    private static final BaseColor BLACK     = new BaseColor(10, 10, 10);
    private static final BaseColor WHITE     = BaseColor.WHITE;
    private static final BaseColor LIGHT_BG  = new BaseColor(245, 247, 252);
    private static final BaseColor MUTED     = new BaseColor(120, 120, 120);
    private static final BaseColor BORDER    = new BaseColor(200, 210, 228);
    private static final BaseColor TABLE_ALT = new BaseColor(245, 247, 252);

    // ─── Fuentes ───────────────────────────────────────────────────────────────
    private static final Font F_TINY     = new Font(Font.FontFamily.HELVETICA,  6, Font.NORMAL, MUTED);
    private static final Font F_SMALL    = new Font(Font.FontFamily.HELVETICA,  7, Font.NORMAL, MUTED);
    private static final Font F_SMALL_B  = new Font(Font.FontFamily.HELVETICA,  7, Font.BOLD,   BLACK);
    private static final Font F_NORMAL   = new Font(Font.FontFamily.HELVETICA,  9, Font.NORMAL, BLACK);
    private static final Font F_NORMAL_B = new Font(Font.FontFamily.HELVETICA,  9, Font.BOLD,   BLACK);
    private static final Font F_NORMAL_BL= new Font(Font.FontFamily.HELVETICA,  9, Font.BOLD,   BLUE_MED);
    private static final Font F_MEDIUM   = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD,   BLACK);
    private static final Font F_LARGE    = new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD,   BLACK);
    private static final Font F_XLARGE   = new Font(Font.FontFamily.HELVETICA, 28, Font.BOLD,   BLACK);
    private static final Font F_TITLE_BL = new Font(Font.FontFamily.HELVETICA, 28, Font.BOLD,   BLUE_MED);
    private static final Font F_LABEL    = new Font(Font.FontFamily.HELVETICA,  6, Font.BOLD,   MUTED);
    private static final Font F_TH       = new Font(Font.FontFamily.HELVETICA,  7, Font.BOLD,   WHITE);
    private static final Font F_TD       = new Font(Font.FontFamily.HELVETICA,  8, Font.NORMAL, BLACK);
    private static final Font F_TD_B     = new Font(Font.FontFamily.HELVETICA,  8, Font.BOLD,   BLACK);
    private static final Font F_TOTAL    = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD,   BLUE_MED);
    private static final Font F_TOTAL_LBL= new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD,   BLACK);
    private static final Font F_FOOTER   = new Font(Font.FontFamily.HELVETICA,  7, Font.NORMAL, MUTED);
    private static final Font F_FOOTER_B = new Font(Font.FontFamily.HELVETICA,  7, Font.BOLD,   BLACK);

    private static final float PAGE_W = PageSize.A4.getWidth();
    private static final float PAD    = 42;

    public byte[] generateFacturaPdf(ComprobanteModel comprobante, BillingRequest billing) {
        try {
            Document doc = new Document(PageSize.A4, PAD, PAD, PAD, PAD);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            doc.open();

            PdfContentByte cb = writer.getDirectContent();
            float usableW = PAGE_W - PAD * 2;

            // ══ HEADER ════════════════════════════════════════════════════════
            PdfPTable header = new PdfPTable(2);
            header.setWidthPercentage(100);
            header.setWidths(new float[]{1.2f, 1f});
            header.setSpacingAfter(18);

            // Izquierda: Logo + datos emisor
            PdfPCell leftCell = new PdfPCell();
            leftCell.setBorder(Rectangle.NO_BORDER);
            leftCell.setPadding(0);

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
                addDataRow(logoTable, "PLATAFORMA DE TICKETS PREMIUM", F_LABEL);
                addSpacerRow(logoTable, 6);
                addDataRow(logoTable, "RUC: 20000000001", F_SMALL_B);
                addDataRow(logoTable, "Dir: Av. Javier Prado Este 4200, San Borja, Lima", F_SMALL);
                addDataRow(logoTable, "Tel: +51 01 234-5678  ·  Web: eventperu.com", F_SMALL);
                leftCell.addElement(logoTable);
            } catch (Exception e) {
                leftCell.addElement(new Paragraph("EventPeru", F_LARGE));
            }
            header.addCell(leftCell);

            // Derecha: Tipo + número — FACTURA usa azul
            PdfPCell rightCell = new PdfPCell();
            rightCell.setBorder(Rectangle.NO_BORDER);
            rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            rightCell.setPadding(0);

            PdfPTable rightTable = new PdfPTable(1);
            rightTable.setWidthPercentage(100);

            PdfPCell typeLabel = new PdfPCell(new Phrase(
                    "FACTURA ELECTRÓNICA",
                    new Font(Font.FontFamily.HELVETICA, 7, Font.BOLD,
                            new BaseColor(80, 80, 80))));
            typeLabel.setBorder(Rectangle.NO_BORDER);
            typeLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
            typeLabel.setPaddingBottom(4);
            rightTable.addCell(typeLabel);

            String[] parts  = comprobante.getNumeroComprobante().split("-");
            String serie    = parts.length > 0 ? parts[0] : "F001";
            String numero   = parts.length > 1
                    ? String.format("%08d", Integer.parseInt(parts[1])) : "00000001";

            PdfPCell numCell = new PdfPCell();
            numCell.setBorder(Rectangle.NO_BORDER);
            numCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            Paragraph numPara = new Paragraph();
            numPara.setAlignment(Element.ALIGN_RIGHT);
            numPara.add(new Chunk(serie + "-", F_XLARGE));
            numPara.add(new Chunk(numero, F_TITLE_BL));   // azul para factura
            numCell.addElement(numPara);
            rightTable.addCell(numCell);

            PdfPCell subSerie = new PdfPCell(new Phrase(
                    "Factura electrónica · Serie " + serie, F_SMALL));
            subSerie.setBorder(Rectangle.NO_BORDER);
            subSerie.setHorizontalAlignment(Element.ALIGN_RIGHT);
            subSerie.setPaddingBottom(10);
            rightTable.addCell(subSerie);

            String fechaStr = comprobante.getFechaEmision() != null
                    ? comprobante.getFechaEmision()
                    .format(DateTimeFormatter.ofPattern("dd 'de' MMMM, yyyy")) : "—";
            String horaStr = comprobante.getFechaEmision() != null
                    ? comprobante.getFechaEmision()
                    .format(DateTimeFormatter.ofPattern("HH:mm:ss")) + " hrs" : "—";

            addRightCell(rightTable, "Fecha de emisión", F_SMALL);
            addRightCell(rightTable, fechaStr,
                    new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BLACK));
            addRightCell(rightTable, "Hora: " + horaStr, F_SMALL);

            // Badge SUNAT azul para factura
            PdfPCell sunatBadge = new PdfPCell();
            sunatBadge.setBorder(Rectangle.BOX);
            sunatBadge.setBorderColor(BLUE_MED);
            sunatBadge.setBorderWidth(0.8f);
            sunatBadge.setPadding(4);
            sunatBadge.setHorizontalAlignment(Element.ALIGN_RIGHT);
            Paragraph sunatPara = new Paragraph();
            sunatPara.setAlignment(Element.ALIGN_RIGHT);
            sunatPara.add(new Chunk("✓ SUNAT Procesado  ·  Hash verificado", F_NORMAL_BL));
            sunatBadge.addElement(sunatPara);
            rightTable.addCell(sunatBadge);

            rightCell.addElement(rightTable);
            header.addCell(rightCell);
            doc.add(header);

            // ══ DATOS COMPRADOR / VENDEDOR ════════════════════════════════════
            PdfPTable parties = new PdfPTable(2);
            parties.setWidthPercentage(100);
            parties.setWidths(new float[]{1f, 1f});
            parties.setSpacingBefore(14);
            parties.setSpacingAfter(14);

            // Comprador — en factura muestra RUC y Razón Social
            PdfPCell buyerCell = new PdfPCell();
            buyerCell.setBorder(Rectangle.RIGHT);
            buyerCell.setBorderColor(BORDER);
            buyerCell.setBorderWidthRight(0.5f);
            buyerCell.setPaddingRight(16);
            buyerCell.setPaddingBottom(12);

            Paragraph buyerLbl = new Paragraph("DATOS DEL CLIENTE (RECEPTOR)", F_LABEL);
            buyerLbl.setSpacingAfter(6);
            buyerCell.addElement(buyerLbl);

            // Razón Social — campo clave en factura
            String razonSocial = billing.getRazonSocial() != null
                    ? billing.getRazonSocial() : comprobante.getClienteNombre();
            Paragraph buyerName = new Paragraph(razonSocial,
                    new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD, BLACK));
            buyerName.setSpacingAfter(8);
            buyerCell.addElement(buyerName);

            // En factura el documento es RUC
            addIconRow(buyerCell, "RUC: " + billing.getClienteDocumento(), F_NORMAL_B);
            addIconRow(buyerCell, comprobante.getClienteEmail(), F_NORMAL);
            if (billing.getClienteDireccion() != null)
                addIconRow(buyerCell, billing.getClienteDireccion(), F_NORMAL);

            PdfPTable rucBadgeC = new PdfPTable(1);
            rucBadgeC.setWidthPercentage(70);
            PdfPCell rucCellC = new PdfPCell(new Phrase(
                    "RUC · " + billing.getClienteDocumento(), F_NORMAL_B));
            rucCellC.setBorder(Rectangle.BOX);
            rucCellC.setBorderColor(BORDER);
            rucCellC.setBorderWidth(0.8f);
            rucCellC.setPadding(5);
            rucCellC.setBackgroundColor(LIGHT_BG);
            rucBadgeC.addCell(rucCellC);
            buyerCell.addElement(rucBadgeC);
            parties.addCell(buyerCell);

            // Vendedor
            PdfPCell sellerCell = new PdfPCell();
            sellerCell.setBorder(Rectangle.NO_BORDER);
            sellerCell.setPaddingLeft(16);
            sellerCell.setPaddingBottom(12);

            Paragraph sellerLbl = new Paragraph("DATOS DEL EMISOR", F_LABEL);
            sellerLbl.setSpacingAfter(6);
            sellerCell.addElement(sellerLbl);

            Paragraph sellerName = new Paragraph("EventPeru S.A.C.",
                    new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD, BLACK));
            sellerName.setSpacingAfter(8);
            sellerCell.addElement(sellerName);

            addIconRow(sellerCell, "RUC: 20000000001", F_NORMAL);
            addIconRow(sellerCell, "Av. Javier Prado Este 4200, San Borja", F_NORMAL);
            addIconRow(sellerCell, "Operador autorizado SUNAT", F_NORMAL);

            PdfPTable rucBadge = new PdfPTable(1);
            rucBadge.setWidthPercentage(70);
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

            // ══ EVENTO ════════════════════════════════════════════════════════
            PdfPTable eventBar = new PdfPTable(1);
            eventBar.setWidthPercentage(100);
            eventBar.setSpacingAfter(10);

            PdfPCell eventCell = new PdfPCell();
            eventCell.setBorder(Rectangle.BOTTOM);
            eventCell.setBorderColor(BORDER);
            eventCell.setBorderWidthBottom(0.5f);
            eventCell.setPaddingBottom(8);
            eventCell.setPaddingTop(4);

            Paragraph eventName = new Paragraph(
                    billing.getEventoNombre() != null
                            ? billing.getEventoNombre()
                            : comprobante.getEventoNombre(),
                    new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD,
                            new BaseColor(160, 160, 160)));
            eventName.setSpacingBefore(2);

            Paragraph eventMeta = new Paragraph();
            if (billing.getEventoFecha() != null)
                eventMeta.add(new Chunk(billing.getEventoFecha() + "   ", F_SMALL_B));
            if (billing.getEventoLugar() != null)
                eventMeta.add(new Chunk(billing.getEventoLugar(), F_SMALL));
            eventMeta.setSpacingBefore(2);

            eventCell.addElement(eventName);
            eventCell.addElement(eventMeta);
            eventBar.addCell(eventCell);
            doc.add(eventBar);

            // ══ TABLA ITEMS ═══════════════════════════════════════════════════
            PdfPTable itemsTable = new PdfPTable(5);
            itemsTable.setWidthPercentage(100);
            itemsTable.setWidths(new float[]{0.4f, 2.8f, 0.7f, 0.9f, 0.9f});
            itemsTable.setSpacingAfter(0);

            for (String h : new String[]{"#", "DESCRIPCIÓN", "CANT.", "P. UNIT.", "SUBTOTAL"}) {
                PdfPCell thCell = new PdfPCell(new Phrase(h, F_TH));
                thCell.setBackgroundColor(BLUE);          // azul oscuro para factura
                thCell.setPadding(6);
                thCell.setBorder(Rectangle.NO_BORDER);
                thCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                itemsTable.addCell(thCell);
            }

            List<BillingItemRequest> items = billing.getItems();
            double subtotalTotal = 0;

            if (items != null && !items.isEmpty()) {
                int idx = 1;
                for (BillingItemRequest item : items) {
                    BaseColor rowBg = (idx % 2 == 0) ? TABLE_ALT : WHITE;

                    addItemCell(itemsTable, String.format("0%d", idx),
                            Element.ALIGN_CENTER, F_TD, rowBg);

                    PdfPCell descCell = new PdfPCell();
                    descCell.setBackgroundColor(rowBg);
                    descCell.setBorder(Rectangle.NO_BORDER);
                    descCell.setPadding(6);
                    descCell.addElement(new Paragraph(
                            item.getDescripcion() != null ? item.getDescripcion() : "—", F_TD_B));
                    String detalle = "";
                    if (billing.getEventoNombre() != null) detalle += billing.getEventoNombre();
                    if (billing.getEventoFecha()  != null) detalle += " · " + billing.getEventoFecha();
                    if (billing.getEventoLugar()  != null) detalle += " · " + billing.getEventoLugar();
                    if (!detalle.isEmpty())
                        descCell.addElement(new Paragraph(detalle,
                                new Font(Font.FontFamily.HELVETICA, 7, Font.NORMAL, MUTED)));
                    itemsTable.addCell(descCell);

                    double pu  = item.getPrecioUnitario() / 1.18; // sin IGV para factura
                    double sub = pu * item.getCantidad();
                    subtotalTotal += sub;

                    addItemCell(itemsTable, String.valueOf(item.getCantidad()),
                            Element.ALIGN_CENTER, F_TD, rowBg);
                    addItemCell(itemsTable, String.format("S/ %.2f", pu),
                            Element.ALIGN_RIGHT, F_TD, rowBg);
                    addItemCell(itemsTable, String.format("S/ %.2f", sub),
                            Element.ALIGN_RIGHT, F_TD_B, rowBg);
                    idx++;
                }
            }
            doc.add(itemsTable);

            // ══ TOTALES ═══════════════════════════════════════════════════════
            double igv   = subtotalTotal * 0.18;
            double total = comprobante.getTotalVenta() != null
                    ? comprobante.getTotalVenta() : subtotalTotal + igv;

            PdfPTable totals = new PdfPTable(2);
            totals.setWidthPercentage(45);
            totals.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totals.setWidths(new float[]{1.4f, 1f});
            totals.setSpacingBefore(0);
            totals.setSpacingAfter(16);

            addTotalRow(totals, "Op. Gravada",
                    String.format("S/ %.2f", subtotalTotal), F_NORMAL, F_NORMAL, WHITE);
            addTotalRow(totals, "IGV (18%)",
                    String.format("S/ %.2f", igv), F_NORMAL_BL, F_NORMAL_BL,
                    new BaseColor(240, 244, 255));
            addTotalRow(totals, "Descuentos", "S/ 0.00", F_NORMAL, F_NORMAL, WHITE);

            PdfPCell totalLblCell = new PdfPCell(new Phrase("IMPORTE TOTAL", F_TOTAL_LBL));
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

            // ══ REFERENCIA ════════════════════════════════════════════════════
            PdfPTable txnTable = new PdfPTable(1);
            txnTable.setWidthPercentage(100);
            txnTable.setSpacingAfter(16);

            PdfPCell txnCell = new PdfPCell();
            txnCell.setBorder(Rectangle.BOX);
            txnCell.setBorderColor(BORDER);
            txnCell.setBorderWidth(0.5f);
            txnCell.setBackgroundColor(LIGHT_BG);
            txnCell.setPadding(10);

            addTxnRow(txnCell, "Referencia:", comprobante.getTransactionReference());
            addTxnRow(txnCell, "Comprobante:", comprobante.getNumeroComprobante());
            addTxnRow(txnCell, "Tickets enviados a:", comprobante.getClienteEmail());
            txnTable.addCell(txnCell);
            doc.add(txnTable);

            // ══ FOOTER ════════════════════════════════════════════════════════
            PdfPTable footer = new PdfPTable(2);
            footer.setWidthPercentage(100);
            footer.setWidths(new float[]{1.8f, 1f});

            PdfPCell footerLeft = new PdfPCell();
            footerLeft.setBorder(Rectangle.NO_BORDER);
            Paragraph footerNote = new Paragraph("Representación impresa de la ", F_FOOTER);
            footerNote.add(new Chunk("Factura Electrónica.", F_FOOTER_B));
            footerNote.add(new Chunk(" Puede consultar la validez en ", F_FOOTER));
            footerNote.add(new Chunk("sunat.gob.pe", F_FOOTER_B));
            footerNote.add(new Chunk(" · Soporte: soporte@eventperu.com", F_FOOTER));
            footerLeft.addElement(footerNote);
            footer.addCell(footerLeft);

            PdfPCell footerRight = new PdfPCell();
            footerRight.setBorder(Rectangle.NO_BORDER);
            footerRight.setHorizontalAlignment(Element.ALIGN_RIGHT);
            String hashText = comprobante.getSunatMensaje() != null
                    ? "Hash: " + comprobante.getSunatMensaje()
                    .substring(0, Math.min(20, comprobante.getSunatMensaje().length())) + "..."
                    : "Hash: —";
            footerRight.addElement(new Paragraph(hashText, F_FOOTER));
            footerRight.addElement(new Paragraph(
                    "Emitido por EventPeru · Sistema FE v2.4", F_FOOTER));
            footer.addCell(footerRight);
            doc.add(footer);

            doc.close();
            log.info("PDF factura generado: {}", comprobante.getNumeroComprobante());
            return out.toByteArray();

        } catch (Exception e) {
            log.error("Error generando PDF factura: {}", e.getMessage());
            throw new RuntimeException("Error generando factura PDF", e);
        }
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────
    private void addDataRow(PdfPTable t, String text, Font f) {
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setBorder(Rectangle.NO_BORDER);
        c.setPaddingBottom(2);
        t.addCell(c);
    }

    private void addSpacerRow(PdfPTable t, float h) {
        PdfPCell c = new PdfPCell(new Phrase(" "));
        c.setBorder(Rectangle.NO_BORDER);
        c.setFixedHeight(h);
        t.addCell(c);
    }

    private void addRightCell(PdfPTable t, String text, Font f) {
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setBorder(Rectangle.NO_BORDER);
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c.setPaddingBottom(3);
        t.addCell(c);
    }

    private void addIconRow(PdfPCell cell, String text, Font f) {
        Paragraph p = new Paragraph(text, f);
        p.setSpacingAfter(3);
        cell.addElement(p);
    }

    private void addItemCell(PdfPTable t, String text, int align, Font f, BaseColor bg) {
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

    private void addTxnRow(PdfPCell cell, String label, String value) {
        Paragraph p = new Paragraph();
        p.add(new Chunk(label + "  ", F_SMALL));
        p.add(new Chunk(value != null ? value : "—", F_SMALL_B));
        p.setSpacingAfter(3);
        cell.addElement(p);
    }
}
