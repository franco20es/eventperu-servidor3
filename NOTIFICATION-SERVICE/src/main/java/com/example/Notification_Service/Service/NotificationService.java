package com.example.Notification_Service.Service;

import com.example.Notification_Service.dto.Request.NotificationRequest;
import com.example.Notification_Service.dto.Request.TicketEmailRequest;
import com.example.Notification_Service.dto.Response.NotificationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface NotificationService {

    NotificationResponse crearNotificacion(NotificationRequest request);

    Page<NotificationResponse> obtenerPorUsuario(String usuarioId, Pageable pageable);

    List<NotificationResponse> obtenerNoLeidas(String usuarioId);

    long contarNoLeidas(String usuarioId);

    void marcarComoLeida(String id);

    void marcarTodasLeidas(String usuarioId);

    void eliminar(String id);

    void enviarTicketEmail(TicketEmailRequest request);
}