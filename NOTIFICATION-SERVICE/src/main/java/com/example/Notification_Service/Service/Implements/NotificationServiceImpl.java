package com.example.Notification_Service.Service.Implements;

import com.example.Notification_Service.Model.NotificationModel;
import com.example.Notification_Service.Model.TipoNotificacion;
import com.example.Notification_Service.Repository.NotificationRepository;
import com.example.Notification_Service.Service.NotificationService;
import com.example.Notification_Service.dto.Request.NotificationRequest;
import com.example.Notification_Service.dto.Request.TicketEmailRequest;
import com.example.Notification_Service.dto.Response.NotificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailServiceImpl emailService;
    private final WhatsAppServiceImpl whatsAppService;
    private final PreferenciasService preferenciasService;

    @Override
    @Transactional
    public NotificationResponse crearNotificacion(NotificationRequest request) {

        log.info("Creando notificación para usuario: {}, tipo: {}", request.getUsuarioId(), request.getTipo());

        // Crear notificación en DB
        NotificationModel notificacion = NotificationModel.builder()
                .usuarioId(request.getUsuarioId())
                .titulo(request.getTitulo())
                .mensaje(request.getMensaje())
                .tipo(TipoNotificacion.valueOf(request.getTipo().toUpperCase()))
                .eventoId(request.getEventoId())
                .eventoNombre(request.getEventoNombre())
                .emailDestino(request.getEmailDestino())
                .telefonoDestino(request.getTelefonoDestino())
                .build();

        // Enviar email si tiene destino
        boolean emailEnviado = false;
        if (request.getEmailDestino() != null && !request.getEmailDestino().isBlank()
                && preferenciasService.debeEnviarEmail(request.getUsuarioId())) {
            try {
                emailService.enviarEmail(
                        request.getEmailDestino(),
                        request.getTitulo(),
                        request.getMensaje()
                );
                emailEnviado = true;
            } catch (Exception e) {
                log.error("Error enviando email a {}: {}", request.getEmailDestino(), e.getMessage());
            }
        }

        //metodo pra emviar whasapt
        // Enviar WhatsApp
        if (request.getTelefonoDestino() != null && !request.getTelefonoDestino().isBlank()
                && preferenciasService.debeEnviarSms(request.getUsuarioId())) {
            try {
                String mensajeWhatsapp = """
                    NUEVO EVENTO DISPONIBLE

                    Evento: %s

                    %s

                    Compra tus entradas ahora.
                """.formatted(
                        request.getEventoNombre(),
                        request.getMensaje()
                );

                whatsAppService.enviarMensaje(
                        request.getTelefonoDestino(),
                        mensajeWhatsapp
                );

                log.info("WhatsApp enviado a: {}", request.getTelefonoDestino());

            } catch (Exception e) {

                log.error("Error enviando WhatsApp: {}", e.getMessage());
            }
        }

        notificacion.setEmailEnviado(emailEnviado);
        NotificationModel guardada = notificationRepository.save(notificacion);

        log.info("Notificación creada: id={}", guardada.getId());

        return toResponse(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> obtenerPorUsuario(String usuarioId, Pageable pageable) {
        return notificationRepository.findByUsuarioIdOrderByCreatedAtDesc(usuarioId, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> obtenerNoLeidas(String usuarioId) {
        return notificationRepository.findByUsuarioIdAndLeidoFalseOrderByCreatedAtDesc(usuarioId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long contarNoLeidas(String usuarioId) {
        return notificationRepository.countByUsuarioIdAndLeidoFalse(usuarioId);
    }

    @Override
    @Transactional
    public void marcarComoLeida(String id) {
        notificationRepository.findById(id).ifPresent(n -> {
            n.setLeido(true);
            notificationRepository.save(n);
        });
    }

    @Override
    @Transactional
    public void marcarTodasLeidas(String usuarioId) {
        List<NotificationModel> noLeidas = notificationRepository
                .findByUsuarioIdAndLeidoFalseOrderByCreatedAtDesc(usuarioId);
        noLeidas.forEach(n -> n.setLeido(true));
        notificationRepository.saveAll(noLeidas);
    }

    @Override
    @Transactional
    public void eliminar(String id) {
        notificationRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void enviarTicketEmail(TicketEmailRequest request) {
        log.info("Enviando ticket {} a: {}", request.getTicketCode(), request.getEmailDestino());

        String html = "<div style='font-family:Arial;max-width:600px;margin:0 auto'>"
                + "<div style='background:#1DB954;padding:20px;text-align:center'>"
                + "<h1 style='color:white;margin:0'>🎟 EventPeru</h1>"
                + "</div>"
                + "<div style='padding:30px'>"
                + "<h2>¡Tu entrada está lista, " + request.getNombreUsuario() + "!</h2>"
                + "<p>Gracias por tu compra. Adjunto encontrarás tu entrada en PDF.</p>"
                + "<div style='background:#f5f5f5;padding:15px;border-radius:8px;margin:20px 0'>"
                + "<p><b>Evento:</b> " + request.getEventoNombre() + "</p>"
                + "<p><b>Fecha:</b> " + request.getEventoFecha() + "</p>"
                + "<p><b>Lugar:</b> " + request.getEventoLugar() + "</p>"
                + "<p><b>Código:</b> " + request.getTicketCode() + "</p>"
                + "<p><b>Orden:</b> " + request.getOrderReference() + "</p>"
                + "</div>"
                + "<p style='color:#888;font-size:12px'>Presenta el QR en la entrada del evento.</p>"
                + "</div>"
                + "</div>";

        if (request.getPdfTicket() != null) {
            emailService.enviarEmailConAdjunto(
                    request.getEmailDestino(),
                    "Tu entrada para " + request.getEventoNombre(),
                    html,
                    request.getPdfTicket(),
                    "ticket-" + request.getTicketCode() + ".pdf"
            );
        }

        // Guardar notificación en BD
        NotificationModel notificacion = NotificationModel.builder()
                .usuarioId(request.getUsuarioId())
                .titulo("Tu entrada para " + request.getEventoNombre())
                .mensaje("Ticket " + request.getTicketCode() + " generado correctamente")
                .tipo(TipoNotificacion.COMPRA_CONFIRMADA)
                .eventoNombre(request.getEventoNombre())
                .emailDestino(request.getEmailDestino())
                .emailEnviado(true)
                .build();
        notificationRepository.save(notificacion);
        log.info("Ticket enviado y notificación guardada para: {}", request.getEmailDestino());
    }

    private NotificationResponse toResponse(NotificationModel model) {
        return NotificationResponse.builder()
                .id(model.getId())
                .usuarioId(model.getUsuarioId())
                .titulo(model.getTitulo())
                .mensaje(model.getMensaje())
                .tipo(model.getTipo().toString())
                .leido(model.getLeido())
                .eventoId(model.getEventoId())
                .eventoNombre(model.getEventoNombre())
                .emailEnviado(model.getEmailEnviado())
                .createdAt(model.getCreatedAt())
                .build();
    }
}