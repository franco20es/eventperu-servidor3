package com.example.payment_service.Service.Implements;

import com.example.payment_service.Model.TicketModel;
import com.example.payment_service.Model.TicketStatus;
import com.example.payment_service.Repository.TicketRepository;

import com.example.payment_service.Service.AdminTicketService;
import com.example.payment_service.dto.Response.ReporteEventoResponse;
import com.example.payment_service.dto.Response.TicketAdminResponse;
import com.example.payment_service.dto.Response.TicketKpisResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminTicketServiceImpl implements AdminTicketService {

    private final TicketRepository ticketRepo;

    private static final DateTimeFormatter FMT_FECHA  =
            DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter FMT_COMPRA =
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
    private static final String[] COLORES =
            {"var(--bm)", "var(--or)", "var(--ok)", "var(--wa)", "#9B59B6"};

    // ── KPIs ──────────────────────────────────────────────────────────────────
    @Override
    public TicketKpisResponse getKpis() {
        List<TicketModel> all = ticketRepo.findAll();

        double recaudado = all.stream()
                .filter(t -> t.getStatus() == TicketStatus.ACTIVE ||
                        t.getStatus() == TicketStatus.USED)
                .mapToDouble(t -> t.getPrice() != null ? t.getPrice().doubleValue() : 0)
                .sum();

        return TicketKpisResponse.builder()
                .total(all.size())
                .activos(count(all, TicketStatus.ACTIVE))
                .usados(count(all, TicketStatus.USED))
                .cancelados(count(all, TicketStatus.CANCELLED))
                .pendientes(count(all, TicketStatus.PENDING))
                .recaudado("S/ " + format(recaudado))
                .recaudadoNum(round(recaudado))
                .build();
    }

    @Override
    public List<Map<String, Object>> getEventosConTickets() {
        return ticketRepo.findAll().stream()
                .collect(Collectors.groupingBy(TicketModel::getEventId))
                .entrySet().stream()
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("eventoId", e.getKey());
                    m.put("nombre",   e.getValue().get(0).getEventName());
                    m.put("total",    e.getValue().size());
                    return m;
                })
                .sorted((a, b) -> Integer.compare((int)b.get("total"), (int)a.get("total")))
                .collect(Collectors.toList());
    }


    // ── Listar paginado ───────────────────────────────────────────────────────
    @Override
    public Page<TicketAdminResponse> listar(String busqueda, String status,
                                            String eventoId, Pageable pageable) {
        List<TicketModel> all = ticketRepo.findAll(
                Sort.by(Sort.Direction.DESC, "createdAt"));

        List<TicketModel> filtrado = all.stream()
                .filter(t -> matchBusqueda(t, busqueda))
                .filter(t -> matchStatus(t, status))
                .filter(t -> eventoId == null || eventoId.isBlank() ||
                        eventoId.equals(t.getEventId()))
                .collect(Collectors.toList());

        int total = filtrado.size();
        int from  = (int) pageable.getOffset();
        int to    = Math.min(from + pageable.getPageSize(), total);

        List<TicketAdminResponse> pagina = from > total
                ? Collections.emptyList()
                : filtrado.subList(from, to).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(pagina, pageable, total);
    }

    // ── Detalle ───────────────────────────────────────────────────────────────
    @Override
    public TicketAdminResponse getDetalle(String id) {
        return toResponse(findById(id));
    }

    // ── Cancelar ──────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public TicketAdminResponse cancelar(String id) {
        TicketModel ticket = findById(id);
        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new RuntimeException("El ticket ya está cancelado");
        }
        if (ticket.getStatus() == TicketStatus.USED) {
            throw new RuntimeException("No se puede cancelar un ticket ya usado");
        }
        ticket.setStatus(TicketStatus.CANCELLED);
        log.info("Ticket cancelado: {}", ticket.getCode());
        return toResponse(ticketRepo.save(ticket));
    }

    // ── Reporte por evento ────────────────────────────────────────────────────
    @Override
    public ReporteEventoResponse getReporteEvento(String eventoId) {
        List<TicketModel> tickets = ticketRepo.findByEventId(eventoId);

        if (tickets.isEmpty()) {
            return ReporteEventoResponse.builder()
                    .eventoId(eventoId).eventoNombre("Evento no encontrado")
                    .totalTickets(0).zonas(Collections.emptyList()).build();
        }

        String eventoNombre = tickets.get(0).getEventName();

        double recaudadoTotal = tickets.stream()
                .filter(t -> t.getStatus() == TicketStatus.ACTIVE ||
                        t.getStatus() == TicketStatus.USED)
                .mapToDouble(t -> t.getPrice() != null ? t.getPrice().doubleValue() : 0)
                .sum();

        long vendidos  = tickets.stream().filter(t -> t.getStatus() != TicketStatus.CANCELLED &&
                t.getStatus() != TicketStatus.PENDING).count();
        long usados    = count(tickets, TicketStatus.USED);
        long cancelados = count(tickets, TicketStatus.CANCELLED);
        double pct = tickets.size() > 0 ? (double) vendidos / tickets.size() * 100 : 0;

        // agrupar por zona (ticketType)
        Map<String, List<TicketModel>> porZona = tickets.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getTicketType() != null ? t.getTicketType() : "General"));

        int[] idx = {0};
        List<ReporteEventoResponse.ZonaReporteResponse> zonas = porZona.entrySet().stream()
                .map(e -> {
                    List<TicketModel> zt = e.getValue();
                    long zVendidos   = zt.stream().filter(t -> t.getStatus() != TicketStatus.CANCELLED &&
                            t.getStatus() != TicketStatus.PENDING).count();
                    long zUsados     = count(zt, TicketStatus.USED);
                    long zCancelados = count(zt, TicketStatus.CANCELLED);
                    long zDisp       = zt.size() - zVendidos;
                    double zRec      = zt.stream()
                            .filter(t -> t.getStatus() == TicketStatus.ACTIVE ||
                                    t.getStatus() == TicketStatus.USED)
                            .mapToDouble(t -> t.getPrice() != null ? t.getPrice().doubleValue() : 0)
                            .sum();
                    double zPct = zt.size() > 0 ? round((double) zVendidos / zt.size() * 100) : 0;

                    ReporteEventoResponse.ZonaReporteResponse z =
                            ReporteEventoResponse.ZonaReporteResponse.builder()
                                    .nombre(e.getKey())
                                    .color(COLORES[idx[0] % COLORES.length])
                                    .total(zt.size())
                                    .vendidos(zVendidos)
                                    .usados(zUsados)
                                    .cancelados(zCancelados)
                                    .disponibles(Math.max(zDisp, 0))
                                    .pct(zPct)
                                    .recaudado("S/ " + format(zRec))
                                    .recaudadoNum(round(zRec))
                                    .build();
                    idx[0]++;
                    return z;
                })
                .sorted(Comparator.comparingDouble(
                        ReporteEventoResponse.ZonaReporteResponse::getRecaudadoNum).reversed())
                .collect(Collectors.toList());

        return ReporteEventoResponse.builder()
                .eventoId(eventoId)
                .eventoNombre(eventoNombre)
                .totalTickets(tickets.size())
                .ticketsVendidos(vendidos)
                .ticketsUsados(usados)
                .ticketsCancelados(cancelados)
                .recaudado("S/ " + format(recaudadoTotal))
                .recaudadoNum(round(recaudadoTotal))
                .pctOcupacion(round(pct))
                .zonas(zonas)
                .build();
    }

    // ── Exportar Excel ────────────────────────────────────────────────────────
    @Override
    public byte[] exportarExcel(String status, String eventoId) {
        List<TicketModel> filtrado = ticketRepo.findAll(
                        Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .filter(t -> matchStatus(t, status))
                .filter(t -> eventoId == null || eventoId.isBlank() ||
                        eventoId.equals(t.getEventId()))
                .collect(Collectors.toList());

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Tickets");

            CellStyle hStyle = headerStyle(wb);
            CellStyle mStyle = moneyStyle(wb);

            String[] cols = {"Código", "Comprador", "Email", "Evento",
                    "Zona", "Precio (S/)", "Estado", "Usado", "Fecha Compra"};
            int[]   anchos = {5000, 5000, 7000, 6000, 4000, 3500, 3500, 3500, 5500};

            Row hRow = sheet.createRow(0);
            for (int i = 0; i < cols.length; i++) {
                Cell c = hRow.createCell(i);
                c.setCellValue(cols[i]);
                c.setCellStyle(hStyle);
                sheet.setColumnWidth(i, anchos[i]);
            }

            int rowNum = 1;
            double totalRec = 0;
            for (TicketModel t : filtrado) {
                double precio = t.getPrice() != null ? t.getPrice().doubleValue() : 0;
                if (t.getStatus() == TicketStatus.ACTIVE || t.getStatus() == TicketStatus.USED) {
                    totalRec += precio;
                }
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(nvl(t.getCode()));
                row.createCell(1).setCellValue(nvl(t.getUserName()));
                row.createCell(2).setCellValue(nvl(t.getUserEmail()));
                row.createCell(3).setCellValue(nvl(t.getEventName()));
                row.createCell(4).setCellValue(nvl(t.getTicketType()));
                Cell pc = row.createCell(5); pc.setCellValue(precio); pc.setCellStyle(mStyle);
                row.createCell(6).setCellValue(t.getStatus() != null ? t.getStatus().name() : "");
                row.createCell(7).setCellValue(Boolean.TRUE.equals(t.getUsed()) ? "Sí" : "No");
                row.createCell(8).setCellValue(t.getCreatedAt() != null ? t.getCreatedAt().format(FMT_COMPRA) : "");
            }

            // total
            Row totalRow = sheet.createRow(rowNum + 1);
            totalRow.createCell(4).setCellValue("TOTAL RECAUDADO:");
            Cell tc = totalRow.createCell(5); tc.setCellValue(totalRec); tc.setCellStyle(mStyle);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Error generando Excel tickets: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar reporte Excel");
        }
    }

    // ── Mapper ────────────────────────────────────────────────────────────────
    private TicketAdminResponse toResponse(TicketModel t) {
        String estadoLabel = switch (t.getStatus()) {
            case ACTIVE    -> "Válido";
            case USED      -> "Usado";
            case CANCELLED -> "Cancelado";
            case REFUNDED  -> "Reembolsado";
            default        -> "Pendiente";
        };

        return TicketAdminResponse.builder()
                .id(t.getId())
                .code(nvl(t.getCode()))
                .userId(nvl(t.getUserId()))
                .userName(nvl(t.getUserName()))
                .userEmail(nvl(t.getUserEmail()))
                .eventId(nvl(t.getEventId()))
                .eventName(nvl(t.getEventName()))
                .eventLocation(nvl(t.getEventLocation()))
                .eventDate(t.getEventDate())
                .ticketType(nvl(t.getTicketType()))
                .price(t.getPrice())
                .status(t.getStatus() != null ? t.getStatus().name() : "PENDING")
                .used(t.getUsed())
                .usedAt(t.getUsedAt())
                .createdAt(t.getCreatedAt())
                .precioFormateado(t.getPrice() != null ? "S/ " + format(t.getPrice().doubleValue()) : "S/ 0.00")
                .estadoLabel(estadoLabel)
                .fechaEventoFmt(t.getEventDate() != null ? t.getEventDate().format(FMT_FECHA) : "—")
                .fechaCompraFmt(t.getCreatedAt() != null ? t.getCreatedAt().format(FMT_COMPRA) : "—")
                .build();
    }

    // ── Filtros ───────────────────────────────────────────────────────────────
    private boolean matchBusqueda(TicketModel t, String q) {
        if (q == null || q.isBlank()) return true;
        String ql = q.toLowerCase();
        return contains(t.getCode(), ql) || contains(t.getUserEmail(), ql) ||
                contains(t.getUserName(), ql) || contains(t.getEventName(), ql);
    }

    private boolean matchStatus(TicketModel t, String status) {
        if (status == null || status.isBlank()) return true;
        try { return t.getStatus() == TicketStatus.valueOf(status); }
        catch (Exception e) { return true; }
    }

    // ── Estilos Excel ─────────────────────────────────────────────────────────
    private CellStyle headerStyle(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont(); f.setBold(true); f.setColor(IndexedColors.WHITE.getIndex());
        s.setFont(f);
        s.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        return s;
    }

    private CellStyle moneyStyle(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
        return s;
    }

    // ── Utils ─────────────────────────────────────────────────────────────────
    private TicketModel findById(String id) {
        return ticketRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado: " + id));
    }

    private long count(List<TicketModel> list, TicketStatus s) {
        return list.stream().filter(t -> t.getStatus() == s).count();
    }

    private boolean contains(String campo, String q) {
        return campo != null && campo.toLowerCase().contains(q);
    }




    private double round(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private String format(double v) { return String.format("%,.2f", v); }

    private String nvl(String s) { return s != null ? s : "—"; }
}