package com.example.payment_service.dto.Response;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TicketValidationResponse {

    private boolean valid;
    private boolean used;
    private String message;
    private String ticketCode;
    private String ownerName;
    private String ownerEmail;
    private String eventName;
    private String eventLocation;
    private String eventDate;
    private String ticketType;
}