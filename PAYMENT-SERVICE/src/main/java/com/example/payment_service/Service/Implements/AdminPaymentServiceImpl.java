package com.example.payment_service.Service.Implements;

import com.example.payment_service.Model.PaymentStatusModel;
import com.example.payment_service.Model.PaymentTransactionModel;
import com.example.payment_service.Repository.PaymentTransactionRepository;

import com.example.payment_service.Service.AdminPaymentService;
import com.example.payment_service.dto.Response.IngresoEventoResponse;
import com.example.payment_service.dto.Response.IngresoMesResponse;
import com.example.payment_service.dto.Response.PagoKpisResponse;
import com.example.payment_service.dto.Response.TransaccionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminPaymentServiceImpl implements AdminPaymentService {

    private final PaymentTransactionRepository transactionRepo;

    private static final double COMISION_PCT = 0.10;
    private static final String[] MESES      = {"Ene","Feb","Mar","Abr","May","Jun",
            "Jul","Ago","Sep","Oct","Nov","Dic"};
    private static final String[] COLORES    = {"var(--bm)","var(--or)","var(--ok)",
            "var(--wa)","#9B59B6","#E74C3C"};

    // ── KPIs ──────────────────────────────────────────────────────────────────
    @Override
    public PagoKpisResponse getKpis() {
        List<PaymentTransactionModel> all = transactionRepo.findAll();

        double totalRecaudado = all.stream()
                .filter(t -> t.getStatus() == PaymentStatusModel.COMPLETED)
                .mapToDouble(t -> t.getAmount() != null ? t.getAmount() : 0)
                .sum();

        long exitosos   = all.stream().filter(t -> t.getStatus() == PaymentStatusModel.COMPLETED).count();
        long fallidos   = all.stream().filter(t -> t.getStatus() == PaymentStatusModel.FAILED ||
                t.getStatus() == PaymentStatusModel.CANCELLED).count();
        long reembolsos = all.stream().filter(t -> t.getStatus() == PaymentStatusModel.REFUNDED).count();
        double comisiones = totalRecaudado * COMISION_PCT;

        return PagoKpisResponse.builder()
                .totalRecaudado("S/ " + formatMonto(totalRecaudado))
                .totalRecaudadoNum(redondear(totalRecaudado))
                .pagosExitosos(exitosos)
                .pagosFallidos(fallidos)
                .reembolsos(reembolsos)
                .comisiones("S/ " + formatMonto(comisiones))
                .comisionesNum(redondear(comisiones))
                .build();
    }

    // ── Listar paginado ───────────────────────────────────────────────────────
    @Override
    public Page<TransaccionResponse> listarTransacciones(
            String busqueda, String estado, String eventoId,
            LocalDate fecha, Pageable pageable) {

        // Cargamos todo y filtramos en memoria.
        // Para escalar a millones de registros se recomienda una @Query con Specification.
        List<PaymentTransactionModel> all = transactionRepo.findAll(
                org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));

        List<PaymentTransactionModel> filtrado = all.stream()
                .filter(t -> matchBusqueda(t, busqueda))
                .filter(t -> matchEstado(t, estado))
                .filter(t -> matchEvento(t, eventoId))
                .filter(t -> matchFecha(t, fecha))
                .collect(Collectors.toList());

        int total = filtrado.size();
        int from  = (int) pageable.getOffset();
        int to    = Math.min(from + pageable.getPageSize(), total);

        List<TransaccionResponse> pagina = (from > total)
                ? Collections.emptyList()
                : filtrado.subList(from, to).stream()
                .map(this::toTransaccionResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(pagina, pageable, total);
    }

    // ── Ingresos por mes ──────────────────────────────────────────────────────
    @Override
    public List<IngresoMesResponse> getIngresosPorMes(int anio) {
        double[] totales = new double[12];

        transactionRepo.findAll().stream()
                .filter(t -> t.getStatus() == PaymentStatusModel.COMPLETED
                        && t.getCreatedAt() != null
                        && t.getCreatedAt().getYear() == anio)
                .forEach(t -> {
                    int mes = t.getCreatedAt().getMonthValue() - 1;
                    totales[mes] += t.getAmount() != null ? t.getAmount() : 0;
                });

        List<IngresoMesResponse> result = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            result.add(IngresoMesResponse.builder()
                    .mes(MESES[i])
                    .valor(redondear(totales[i]))
                    .build());
        }
        return result;
    }

    // ── Ingresos por evento ───────────────────────────────────────────────────
    @Override
    public List<IngresoEventoResponse> getIngresosPorEvento() {
        Map<String, double[]> montos  = new LinkedHashMap<>();
        Map<String, String>   nombres = new LinkedHashMap<>();

        transactionRepo.findAll().stream()
                .filter(t -> t.getStatus() == PaymentStatusModel.COMPLETED)
                .forEach(t -> {
                    String eid  = t.getEventId()   != null ? t.getEventId()   : "sin-evento";
                    String name = t.getEventName() != null ? t.getEventName() : "Sin evento";
                    montos.computeIfAbsent(eid, k -> new double[]{0});
                    montos.get(eid)[0] += t.getAmount() != null ? t.getAmount() : 0;
                    nombres.put(eid, name);
                });

        int[] idx = {0};
        return montos.entrySet().stream()
                .map(e -> {
                    double montoNum = redondear(e.getValue()[0]);
                    IngresoEventoResponse r = IngresoEventoResponse.builder()
                            .eventoId(e.getKey())
                            .nombre(nombres.get(e.getKey()))
                            .color(COLORES[idx[0] % COLORES.length])
                            .monto("S/ " + formatMonto(montoNum))
                            .montoNum(montoNum)
                            .build();
                    idx[0]++;
                    return r;
                })
                .sorted(Comparator.comparingDouble(IngresoEventoResponse::getMontoNum).reversed())
                .limit(10)
                .collect(Collectors.toList());
    }

    // ── Detalle ───────────────────────────────────────────────────────────────
    @Override
    public TransaccionResponse getDetalle(String id) {
        PaymentTransactionModel t = transactionRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Transacción no encontrada: " + id));
        return toTransaccionResponse(t);
    }

    // ── Exportar Excel ────────────────────────────────────────────────────────
    @Override
    public byte[] exportarExcel(String estado, String eventoId, LocalDate fecha) {
        List<TransaccionResponse> data = transactionRepo.findAll(
                        org.springframework.data.domain.Sort.by(
                                org.springframework.data.domain.Sort.Direction.DESC, "createdAt"))
                .stream()
                .filter(t -> matchEstado(t, estado))
                .filter(t -> matchEvento(t, eventoId))
                .filter(t -> matchFecha(t, fecha))
                .map(this::toTransaccionResponse)
                .collect(Collectors.toList());

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Transacciones");

            // ── Estilos ──────────────────────────────────────────────
            CellStyle headerStyle = crearEstiloHeader(wb);
            CellStyle moneyStyle  = crearEstiloMonto(wb);
            CellStyle altStyle    = crearEstiloAlternado(wb);

            // ── Encabezados ───────────────────────────────────────────
            String[] cols = {"ID", "Referencia", "Comprador", "Email", "Evento",
                    "Zona", "Cant.", "Monto (S/)", "Comisión (S/)", "Estado", "Fecha"};
            int[]    anchos = {8000, 6000, 5000, 7000, 6000, 4000, 2500, 3500, 3500, 4000, 5000};

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < cols.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(cols[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, anchos[i]);
            }

            // ── Datos ─────────────────────────────────────────────────
            double totalMonto    = 0;
            double totalComision = 0;
            int rowNum = 1;

            for (TransaccionResponse t : data) {
                Row row = sheet.createRow(rowNum);
                CellStyle rowStyle = (rowNum % 2 == 0) ? altStyle : null;

                crearCelda(row, 0, t.getId(),        rowStyle);
                crearCelda(row, 1, t.getReferencia() != null ? t.getReferencia() : "", rowStyle);
                crearCelda(row, 2, t.getComprador(), rowStyle);
                crearCelda(row, 3, t.getEmail(),     rowStyle);
                crearCelda(row, 4, t.getEvento(),    rowStyle);
                crearCelda(row, 5, t.getZona(),      rowStyle);
                row.createCell(6).setCellValue(t.getTickets() != null ? t.getTickets() : 0);

                Cell mc = row.createCell(7);
                mc.setCellValue(t.getMontoNum());
                mc.setCellStyle(moneyStyle);

                Cell cc = row.createCell(8);
                cc.setCellValue(t.getComisionNum());
                cc.setCellStyle(moneyStyle);

                crearCelda(row, 9,  t.getEstado(), rowStyle);
                crearCelda(row, 10, t.getFecha(),  rowStyle);

                if ("EXITOSO".equals(t.getEstado())) {
                    totalMonto    += t.getMontoNum();
                    totalComision += t.getComisionNum();
                }
                rowNum++;
            }

            // ── Fila total ────────────────────────────────────────────
            rowNum++;
            Row totalRow = sheet.createRow(rowNum);
            CellStyle totalStyle = wb.createCellStyle();
            Font totalFont = wb.createFont();
            totalFont.setBold(true);
            totalStyle.setFont(totalFont);
            totalStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            totalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Cell labelCell = totalRow.createCell(6);
            labelCell.setCellValue("TOTAL RECAUDADO:");
            labelCell.setCellStyle(totalStyle);

            Cell totalMontoCell = totalRow.createCell(7);
            totalMontoCell.setCellValue(redondear(totalMonto));
            totalMontoCell.setCellStyle(moneyStyle);

            Cell totalComCell = totalRow.createCell(8);
            totalComCell.setCellValue(redondear(totalComision));
            totalComCell.setCellStyle(moneyStyle);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();

        } catch (Exception e) {
            log.error("Error generando Excel: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar reporte Excel: " + e.getMessage());
        }
    }

    // ── Mapper ────────────────────────────────────────────────────────────────
    private TransaccionResponse toTransaccionResponse(PaymentTransactionModel t) {
        double monto    = t.getAmount() != null ? t.getAmount() : 0;
        double comision = redondear(monto * COMISION_PCT);

        return TransaccionResponse.builder()
                .id(t.getId())
                .referencia(t.getTransactionReference())
                .comprador(nvl(t.getUserName()))
                .email(nvl(t.getUserEmail()))
                .evento(nvl(t.getEventName()))
                .eventoId(nvl(t.getEventId()))
                .zona(nvl(t.getTicketType()))
                .tickets(t.getQuantity())
                .monto("S/ " + formatMonto(monto))
                .montoNum(redondear(monto))
                .comision("S/ " + formatMonto(comision))
                .comisionNum(comision)
                .metodo("Mercado Pago")
                .fecha(t.getCreatedAt() != null ? t.getCreatedAt().toString() : "—")
                .estado(mapEstado(t.getStatus()))
                .build();
    }

    // ── Filtros ───────────────────────────────────────────────────────────────
    private boolean matchBusqueda(PaymentTransactionModel t, String busqueda) {
        if (busqueda == null || busqueda.isBlank()) return true;
        String q = busqueda.toLowerCase();
        return contains(t.getTransactionReference(), q)
                || contains(t.getUserEmail(), q)
                || contains(t.getUserName(),  q)
                || contains(t.getEventName(), q);
    }

    private boolean matchEstado(PaymentTransactionModel t, String estado) {
        if (estado == null || estado.isBlank()) return true;
        try { return t.getStatus() == PaymentStatusModel.valueOf(estado); }
        catch (Exception e) { return true; }
    }

    private boolean matchEvento(PaymentTransactionModel t, String eventoId) {
        if (eventoId == null || eventoId.isBlank()) return true;
        return eventoId.equals(t.getEventId());
    }

    private boolean matchFecha(PaymentTransactionModel t, LocalDate fecha) {
        if (fecha == null) return true;
        return t.getCreatedAt() != null && t.getCreatedAt().toLocalDate().equals(fecha);
    }

    // ── Helpers Excel ─────────────────────────────────────────────────────────
    private CellStyle crearEstiloHeader(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle crearEstiloMonto(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
        return style;
    }

    private CellStyle crearEstiloAlternado(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(IndexedColors.LIGHT_TURQUOISE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private void crearCelda(Row row, int col, String valor, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(valor != null ? valor : "");
        if (style != null) cell.setCellStyle(style);
    }

    // ── Utils ─────────────────────────────────────────────────────────────────
    private String mapEstado(PaymentStatusModel status) {
        if (status == null) return "PENDIENTE";
        return switch (status) {
            case COMPLETED -> "EXITOSO";
            case FAILED    -> "FALLIDO";
            case CANCELLED -> "FALLIDO";
            case REFUNDED  -> "REEMBOLSADO";
            default        -> "PENDIENTE";
        };
    }

    private double redondear(double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private String formatMonto(double valor) {
        return String.format("%,.2f", valor);
    }

    private String nvl(String s) { return s != null ? s : "—"; }

    private boolean contains(String campo, String q) {
        return campo != null && campo.toLowerCase().contains(q);
    }
}