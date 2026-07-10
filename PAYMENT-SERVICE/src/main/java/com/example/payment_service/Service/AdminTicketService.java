package com.example.payment_service.Service;


import com.example.payment_service.dto.Response.ReporteEventoResponse;
import com.example.payment_service.dto.Response.TicketAdminResponse;
import com.example.payment_service.dto.Response.TicketKpisResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface AdminTicketService {

    /** KPIs globales: total, usados, activos, cancelados, recaudado. */
    TicketKpisResponse getKpis();

    /**
     * Lista paginada de tickets con filtros opcionales.
     * @param busqueda  texto libre (código, email, nombre, evento)
     * @param status    filtro por TicketStatus (ACTIVE, USED, CANCELLED, PENDING, REFUNDED)
     * @param eventoId  filtro por evento
     * @param pageable  paginación y ordenamiento
     */
    Page<TicketAdminResponse> listar(String busqueda, String status,
                                     String eventoId, Pageable pageable);

    List<Map<String, Object>> getEventosConTickets();

    /** Detalle de un ticket por su ID. */
    TicketAdminResponse getDetalle(String id);

    /** Cancela un ticket (ACTIVE → CANCELLED). */
    TicketAdminResponse cancelar(String id);

    /** Reporte de ocupación y recaudación por evento. */
    ReporteEventoResponse getReporteEvento(String eventoId);

    /** Exporta tickets filtrados como Excel (.xlsx). */
    byte[] exportarExcel(String status, String eventoId);
}
