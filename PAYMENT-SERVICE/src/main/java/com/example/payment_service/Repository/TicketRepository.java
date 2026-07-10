package com.example.payment_service.Repository;

import com.example.payment_service.Model.TicketModel;
import com.example.payment_service.Model.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<TicketModel, String> {

    Optional<TicketModel> findByCode(String code);

    Optional<TicketModel> findByQrToken(String qrToken);

    List<TicketModel> findByUserId(String userId);

    List<TicketModel> findByUserIdOrderByCreatedAtDesc(String userId);

    List<TicketModel> findByEventId(String eventId);

    List<TicketModel> findByOrderId(String orderId);

    boolean existsByCode(String code);

    long countByEventIdAndStatus(String eventId, TicketStatus status);

    Page<TicketModel> findByUserIdOrderByCreatedAtDesc(
            String userId,
            Pageable pageable
    );
    Page<TicketModel> findByStatusOrderByUsedAtDesc(TicketStatus status, Pageable pageable);

    Page<TicketModel> findByStatusAndEventIdOrderByUsedAtDesc(TicketStatus status, String eventId, Pageable pageable);
}