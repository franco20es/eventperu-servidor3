package com.example.payment_service.Service;


import com.example.payment_service.Model.TicketModel;
import com.example.payment_service.dto.Response.TicketValidationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TicketService {

//    List<TicketModel> getTicketsByUser(String userId);

    Page<TicketModel> getTicketsByUser(
            String userId,
            Pageable pageable
    );

    TicketModel getByCode(String code);

    TicketValidationResponse validateTicket(String qrToken);

    byte[] generateTicketPdf(String ticketId);

    TicketValidationResponse checkTicketStatus(String qrToken);

    Page<TicketModel> getTicketsValidados(String eventId, Pageable pageable);
}