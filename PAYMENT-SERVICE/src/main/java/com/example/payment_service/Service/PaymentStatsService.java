package com.example.payment_service.Service;

import com.example.payment_service.Model.PaymentStatusModel;
import com.example.payment_service.Model.PaymentTransactionModel;
import com.example.payment_service.Repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentStatsService {

    private final PaymentTransactionRepository transactionRepository;

    public Map<String, Object> getStats() {
        List<PaymentTransactionModel> all = transactionRepository.findAll();

        return Map.of(
                "kpis",           buildKpis(all),
                "ingresosPorMes", buildIngresosPorMes(all),
                "ticketsPorMes",  buildTicketsPorMes(all),
                "porEstado",      buildPorEstado(all),
                "topEventos",     buildTopEventos(all),
                "porZona",        buildPorZona(all)
        );
    }

    private Map<String, Object> buildKpis(List<PaymentTransactionModel> all) {
        long total      = all.size();
        long completadas = count(all, PaymentStatusModel.COMPLETED);
        long pendientes  = count(all, PaymentStatusModel.PENDING);
        long fallidas    = count(all, PaymentStatusModel.FAILED);

        double totalRecaudado = all.stream()
                .filter(t -> t.getStatus() == PaymentStatusModel.COMPLETED)
                .mapToDouble(t -> t.getAmount() != null ? t.getAmount() : 0)
                .sum();

        int totalTickets = all.stream()
                .filter(t -> t.getStatus() == PaymentStatusModel.COMPLETED)
                .mapToInt(t -> t.getQuantity() != null ? t.getQuantity() : 0)
                .sum();

        double tasaExito = total > 0
                ? Math.round((completadas * 100.0 / total) * 10) / 10.0
                : 0;

        Map<String, Object> kpis = new LinkedHashMap<>();
        kpis.put("totalTransacciones", total);
        kpis.put("completadas",        completadas);
        kpis.put("pendientes",         pendientes);
        kpis.put("fallidas",           fallidas);
        kpis.put("totalRecaudado",     totalRecaudado);
        kpis.put("totalTickets",       totalTickets);
        kpis.put("tasaExito",          tasaExito);
        return kpis;
    }

    private Map<String, Double> buildIngresosPorMes(List<PaymentTransactionModel> all) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");
        LocalDateTime hace6Meses = LocalDateTime.now().minusMonths(6);
        Map<String, Double> result = new TreeMap<>();

        all.stream()
                .filter(t -> t.getStatus() == PaymentStatusModel.COMPLETED)
                .filter(t -> t.getCreatedAt() != null && t.getCreatedAt().isAfter(hace6Meses))
                .forEach(t -> result.merge(
                        t.getCreatedAt().format(fmt),
                        t.getAmount() != null ? t.getAmount() : 0,
                        Double::sum
                ));
        return result;
    }

    private Map<String, Integer> buildTicketsPorMes(List<PaymentTransactionModel> all) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");
        LocalDateTime hace6Meses = LocalDateTime.now().minusMonths(6);
        Map<String, Integer> result = new TreeMap<>();

        all.stream()
                .filter(t -> t.getStatus() == PaymentStatusModel.COMPLETED)
                .filter(t -> t.getCreatedAt() != null && t.getCreatedAt().isAfter(hace6Meses))
                .forEach(t -> result.merge(
                        t.getCreatedAt().format(fmt),
                        t.getQuantity() != null ? t.getQuantity() : 0,
                        Integer::sum
                ));
        return result;
    }

    private Map<String, Long> buildPorEstado(List<PaymentTransactionModel> all) {
        return all.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getStatus() != null ? t.getStatus().name() : "UNKNOWN",
                        Collectors.counting()
                ));
    }

    private List<Map<String, Object>> buildTopEventos(List<PaymentTransactionModel> all) {
        return all.stream()
                .filter(t -> t.getStatus() == PaymentStatusModel.COMPLETED)
                .filter(t -> t.getEventName() != null)
                .collect(Collectors.groupingBy(
                        PaymentTransactionModel::getEventName,
                        Collectors.summingDouble(t -> t.getAmount() != null ? t.getAmount() : 0)
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(5)
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("nombre",    e.getKey());
                    m.put("recaudado", e.getValue());
                    return m;
                })
                .collect(Collectors.toList());
    }

    private Map<String, Double> buildPorZona(List<PaymentTransactionModel> all) {
        return all.stream()
                .filter(t -> t.getStatus() == PaymentStatusModel.COMPLETED)
                .filter(t -> t.getTicketType() != null)
                .collect(Collectors.groupingBy(
                        PaymentTransactionModel::getTicketType,
                        Collectors.summingDouble(t -> t.getAmount() != null ? t.getAmount() : 0)
                ));
    }

    private long count(List<PaymentTransactionModel> all, PaymentStatusModel status) {
        return all.stream().filter(t -> t.getStatus() == status).count();
    }
}
