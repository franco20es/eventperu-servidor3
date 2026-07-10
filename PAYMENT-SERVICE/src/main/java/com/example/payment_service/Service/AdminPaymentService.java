package com.example.payment_service.Service;



import com.example.payment_service.dto.Response.IngresoEventoResponse;
import com.example.payment_service.dto.Response.IngresoMesResponse;
import com.example.payment_service.dto.Response.PagoKpisResponse;
import com.example.payment_service.dto.Response.TransaccionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface AdminPaymentService {

    /**
     * KPIs generales del módulo de pagos:
     * total recaudado, pagos exitosos, fallidos, reembolsos y comisiones.
     */
    PagoKpisResponse getKpis();

    /**
     * Lista paginada de transacciones con filtros opcionales.
     *
     * @param busqueda  texto libre (referencia, email, nombre, evento)
     * @param estado    filtro por estado (COMPLETED, FAILED, REFUNDED, PENDING, CANCELLED)
     * @param eventoId  filtro por evento
     * @param fecha     filtro por fecha exacta de creación
     * @param pageable  paginación y ordenamiento
     */
    Page<TransaccionResponse> listarTransacciones(
            String busqueda,
            String estado,
            String eventoId,
            LocalDate fecha,
            Pageable pageable);

    /**
     * Ingresos agrupados por mes para el año indicado.
     * Devuelve siempre los 12 meses, con valor 0 si no hay transacciones.
     */
    List<IngresoMesResponse> getIngresosPorMes(int anio);

    /**
     * Top 10 eventos por monto recaudado (solo pagos COMPLETED).
     */
    List<IngresoEventoResponse> getIngresosPorEvento();

    /**
     * Detalle de una transacción por su ID.
     */
    TransaccionResponse getDetalle(String id);

    /**
     * Genera un archivo Excel (.xlsx) con las transacciones filtradas.
     * Listo para ser enviado como ResponseEntity<byte[]>.
     */
    byte[] exportarExcel(String estado, String eventoId, LocalDate fecha);
}