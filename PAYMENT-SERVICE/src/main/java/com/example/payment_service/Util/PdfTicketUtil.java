package com.example.payment_service.Util;

import com.example.payment_service.Model.TicketModel;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Component
@Slf4j
@RequiredArgsConstructor
public class PdfTicketUtil {

    private final QrGeneratorUtil qrGeneratorUtil;

    // ─── Colores ───────────────────────────────────────────────────────────────
    private static final BaseColor GREEN       = new BaseColor(0, 255, 102);
    private static final BaseColor GREEN_DARK  = new BaseColor(0, 180, 70);
    private static final BaseColor BLACK       = new BaseColor(10, 10, 10);
    private static final BaseColor WHITE       = BaseColor.WHITE;
    private static final BaseColor LIGHT_BG    = new BaseColor(248, 250, 248);
    private static final BaseColor STUB_BG     = new BaseColor(240, 245, 240);
    private static final BaseColor MUTED       = new BaseColor(100, 100, 100);
    private static final BaseColor BORDER      = new BaseColor(200, 220, 205);

    // ─── Tipografías ───────────────────────────────────────────────────────────
    private static final Font F_BRAND      = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD,   BLACK);
    private static final Font F_TYPE       = new Font(Font.FontFamily.HELVETICA,  7, Font.BOLD,   BLACK);
    private static final Font F_TITLE      = new Font(Font.FontFamily.HELVETICA, 24, Font.BOLD,   BLACK);
    private static final Font F_SUB        = new Font(Font.FontFamily.HELVETICA,  8, Font.NORMAL, MUTED);
    private static final Font F_DATE       = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD,   GREEN_DARK);
    private static final Font F_TIME       = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, BLACK);
    private static final Font F_VENUE      = new Font(Font.FontFamily.HELVETICA,  7, Font.NORMAL, MUTED);
    private static final Font F_LBL        = new Font(Font.FontFamily.HELVETICA,  6, Font.BOLD,   MUTED);
    private static final Font F_VAL        = new Font(Font.FontFamily.HELVETICA,  9, Font.BOLD,   BLACK);
    private static final Font F_VAL_G      = new Font(Font.FontFamily.HELVETICA,  9, Font.BOLD,   GREEN_DARK);
    private static final Font F_CODE       = new Font(Font.FontFamily.COURIER,    6, Font.NORMAL, MUTED);
    private static final Font SF_LBL       = new Font(Font.FontFamily.HELVETICA,  5, Font.BOLD,   MUTED);
    private static final Font SF_VAL       = new Font(Font.FontFamily.HELVETICA,  9, Font.BOLD,   BLACK);
    private static final Font SF_VAL_G     = new Font(Font.FontFamily.HELVETICA,  9, Font.BOLD,   GREEN_DARK);
    private static final Font SF_TITLE     = new Font(Font.FontFamily.HELVETICA,  8, Font.BOLD,   BLACK);
    private static final Font SF_CODE      = new Font(Font.FontFamily.COURIER,    5, Font.NORMAL, MUTED);

    // ─── Dimensiones ───────────────────────────────────────────────────────────
    private static final float W        = 600;
    private static final float H        = 280;
    private static final float STUB_W   = 150;
    private static final float MAIN_W   = W - STUB_W - 3;
    private static final float STRIP_H  = 6;
    private static final float PAD      = 20;

    public byte[] generateTicketPdf(TicketModel ticket) {
        try {
            Document doc = new Document(new Rectangle(W, H));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            doc.setMargins(0, 0, 0, 0);
            doc.open();

            PdfContentByte cb = writer.getDirectContent();

            // ══════════════════════════════════════════════════════════════════
            // MAIN BODY — fondo blanco limpio
            // ══════════════════════════════════════════════════════════════════
            cb.setColorFill(WHITE);
            cb.rectangle(0, 0, MAIN_W, H);
            cb.fill();

            // Borde exterior sutil
            cb.setColorStroke(BORDER);
            cb.setLineWidth(0.8f);
            cb.rectangle(0.4f, 0.4f, MAIN_W - 0.8f, H - 0.8f);
            cb.stroke();

            // ── FRANJA VERDE TOP ──────────────────────────────────────────────
            cb.setColorFill(GREEN);
            cb.rectangle(0, H - STRIP_H, MAIN_W, STRIP_H);
            cb.fill();

            // ── BANDA LATERAL VERDE (acento izquierdo) ────────────────────────
            cb.setColorFill(GREEN);
            cb.rectangle(0, 0, 4, H - STRIP_H);
            cb.fill();

            float contentY = H - STRIP_H - PAD;

            // ── HEADER ROW ────────────────────────────────────────────────────
            // Brand pill (fondo negro)
            cb.setColorFill(BLACK);
            cb.roundRectangle(PAD + 6, contentY - 16, 82, 17, 8);
            cb.fill();

            // Punto verde
            cb.setColorFill(GREEN);
            cb.circle(PAD + 15, contentY - 7.5f, 3.5f);
            cb.fill();

            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                    new Phrase("EventPeru", new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, WHITE)),
                    PAD + 21, contentY - 11, 0);

            // Pill tipo entrada (fondo verde)
            String typeStr = ticket.getTicketType() != null
                    ? ticket.getTicketType().toUpperCase()
                    : "GENERAL";
            cb.setColorFill(GREEN);
            cb.roundRectangle(MAIN_W - 100, contentY - 16, 92, 17, 8);
            cb.fill();
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                    new Phrase("🎟  " + typeStr, F_TYPE),
                    MAIN_W - 54, contentY - 11, 0);

            // ── NOMBRE DEL EVENTO ─────────────────────────────────────────────
            float nameY = contentY - 48;
            String eventName = ticket.getEventName() != null ? ticket.getEventName() : "Evento";
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                    new Phrase(eventName, F_TITLE), PAD + 6, nameY, 0);

            // Lugar (sub)
            String loc = ticket.getEventLocation() != null
                    ? ticket.getEventLocation().toUpperCase() : "";
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                    new Phrase(loc, F_SUB), PAD + 6, nameY - 14, 0);

            // ── FECHA / HORA ──────────────────────────────────────────────────
            String dateStr = "—";
            String timeStr = "";
            if (ticket.getEventDate() != null) {
                dateStr = ticket.getEventDate()
                        .format(DateTimeFormatter.ofPattern("dd MMMM yyyy")).toUpperCase();
                timeStr = ticket.getEventDate()
                        .format(DateTimeFormatter.ofPattern("HH:mm")) + " hrs";
            }
            float dtY = nameY - 34;
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                    new Phrase(dateStr, F_DATE), PAD + 6, dtY, 0);
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                    new Phrase("  ·  " + timeStr, F_TIME), PAD + 6 + 135, dtY, 0);

            // Venue line
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                    new Phrase("📍  " + (ticket.getEventLocation() != null
                            ? ticket.getEventLocation() : ""), F_VENUE),
                    PAD + 6, dtY - 12, 0);

            // ── QR ────────────────────────────────────────────────────────────
            byte[] qrBytes = qrGeneratorUtil.generateQrBytes(ticket.getQrToken());
            Image qr = Image.getInstance(qrBytes);

            float qrSize = 88;
            float qrX = MAIN_W - 108;
            float qrY = 50;

// Caja fondo
            cb.setColorFill(new BaseColor(230, 240, 232));
            cb.roundRectangle(qrX - 8, qrY - 8, qrSize + 18, qrSize + 18, 10);
            cb.fill();

            cb.setColorFill(WHITE);
            cb.roundRectangle(qrX - 5, qrY - 5, qrSize + 12, qrSize + 12, 8);
            cb.fill();

            cb.setColorStroke(GREEN);
            cb.setLineWidth(1.5f);
            cb.roundRectangle(qrX - 5, qrY - 5, qrSize + 12, qrSize + 12, 8);
            cb.stroke();

// ← RENDERIZAR QR CORRECTO con PdfTemplate
            PdfTemplate qrTemplate = cb.createTemplate(qrSize, qrSize);
            qr.scaleToFit(qrSize, qrSize);
            qr.setAbsolutePosition(0, 0);
            qrTemplate.addImage(qr);
            cb.addTemplate(qrTemplate, qrX, qrY);

// Etiquetas
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                    new Phrase(ticket.getCode(), F_CODE), qrX + qrSize/2, qrY - 12, 0);
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                    new Phrase("Escanear al ingresar", F_CODE), qrX + qrSize/2, qrY - 20, 0);

            // ── FILA DATOS (bottom bar) ───────────────────────────────────────
            float barY = 42;

            // Línea separadora
            cb.setColorStroke(BORDER);
            cb.setLineWidth(0.5f);
            cb.setLineDash(3, 3, 0);
            cb.moveTo(PAD + 6, barY + 20);
            cb.lineTo(MAIN_W - 120, barY + 20);
            cb.stroke();
            cb.setLineDash(1, 0);

            addSeatField(cb, "TITULAR",  ticket.getUserName(),       PAD + 6,   barY, false);
            addSeatField(cb, "ZONA",     ticket.getTicketType(),     PAD + 130, barY, true);
            addSeatField(cb, "PRECIO",
                    "S/ " + (ticket.getPrice() != null
                            ? ticket.getPrice().toPlainString() : "—"),
                    PAD + 240, barY, true);

            // ══════════════════════════════════════════════════════════════════
            // TEAR LINE
            // ══════════════════════════════════════════════════════════════════
            float tearX = MAIN_W;
            cb.setColorStroke(new BaseColor(180, 200, 185));
            cb.setLineDash(4, 4, 0);
            cb.setLineWidth(1f);
            cb.moveTo(tearX + 1.5f, 14);
            cb.lineTo(tearX + 1.5f, H - 14);
            cb.stroke();
            cb.setLineDash(1, 0);

            // Muescas semicírculo
            cb.setColorFill(new BaseColor(235, 235, 235));
            cb.circle(tearX + 1.5f, H, 10);
            cb.fill();
            cb.circle(tearX + 1.5f, 0, 10);
            cb.fill();

            // ══════════════════════════════════════════════════════════════════
            // STUB — fondo muy claro
            // ══════════════════════════════════════════════════════════════════
            float sx = tearX + 3;
            cb.setColorFill(STUB_BG);
            cb.rectangle(sx, 0, STUB_W, H);
            cb.fill();

            // Franja verde top stub
            cb.setColorFill(GREEN);
            cb.rectangle(sx, H - STRIP_H, STUB_W, STRIP_H);
            cb.fill();

            // Franja verde izquierda stub
            cb.setColorFill(GREEN);
            cb.rectangle(sx, 0, 3, H - STRIP_H);
            cb.fill();

            float sp = 14;

            // Título vertical (rotado) en stub
            cb.saveState();
            cb.concatCTM(0, 1, -1, 0, sx + sp + 10, 30);
            String shortName = eventName.length() > 22
                    ? eventName.substring(0, 22) + "…" : eventName;
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                    new Phrase(shortName, SF_TITLE), 0, 0, 0);
            cb.restoreState();

            // Campos stub
            float sfY = H - STRIP_H - 22;
            addStubField(cb, "FECHA",
                    ticket.getEventDate() != null
                            ? ticket.getEventDate()
                            .format(DateTimeFormatter.ofPattern("dd MMM yyyy")).toUpperCase()
                            : "—",
                    sx + sp, sfY, false);
            sfY -= 30;
            addStubField(cb, "HORA",
                    ticket.getEventDate() != null
                            ? ticket.getEventDate()
                            .format(DateTimeFormatter.ofPattern("HH:mm")) + " hrs"
                            : "—",
                    sx + sp, sfY, false);
            sfY -= 30;
            addStubField(cb, "ZONA",
                    ticket.getTicketType() != null ? ticket.getTicketType() : "—",
                    sx + sp, sfY, true);
            sfY -= 30;
            addStubField(cb, "PRECIO",
                    "S/ " + (ticket.getPrice() != null
                            ? ticket.getPrice().toPlainString() : "—"),
                    sx + sp, sfY, true);

            // Código de barras simulado
            float bcY  = 22;
            float bcX  = sx + sp;
            float[] bars = {2,1,3,1,2,1,3,1,2,3,1,2,1,3,2,1,3,1,2,1,3,2,1,2,3,1,2,1};
            float bx = bcX;
            cb.setColorFill(new BaseColor(20, 20, 20));
            for (float bw : bars) {
                cb.rectangle(bx, bcY, bw, 16);
                cb.fill();
                bx += bw + 1.8f;
                if (bx > sx + STUB_W - sp - 4) break;
            }
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                    new Phrase(ticket.getCode(), SF_CODE), bcX, bcY - 7, 0);

            doc.close();
            log.info("PDF ticket generado: {}", ticket.getCode());
            return out.toByteArray();

        } catch (Exception e) {
            log.error("Error generando PDF ticket: {}", e.getMessage());
            throw new RuntimeException("Error generando PDF del ticket", e);
        }
    }

    private void addSeatField(PdfContentByte cb, String label, String value,
                              float x, float y, boolean green) {
        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                new Phrase(label, F_LBL), x, y + 12, 0);
        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                new Phrase(value != null ? value : "—", green ? F_VAL_G : F_VAL),
                x, y, 0);
    }

    private void addStubField(PdfContentByte cb, String label, String value,
                              float x, float y, boolean green) {
        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                new Phrase(label, SF_LBL), x, y + 10, 0);
        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                new Phrase(value != null ? value : "—", green ? SF_VAL_G : SF_VAL),
                x, y, 0);
    }
}