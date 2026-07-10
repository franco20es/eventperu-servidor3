package com.example.Notification_Service.Service.Implements;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String emailRemitente;

    // ─── Email simple (texto) ───
    public void enviarEmail(String to, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, false);
            helper.setFrom(emailRemitente);
            mailSender.send(message);
            log.info("Email enviado a: {}", to);
        } catch (Exception e) {
            log.error("Error enviando email a {}: {}", to, e.getMessage());
            throw new RuntimeException("Error enviando email", e);
        }
    }

    // ─── Email con adjunto PDF ───
    public void enviarEmailConAdjunto(String to, String subject, String body,
                                      byte[] adjunto, String nombreAdjunto) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true); // true = HTML
            helper.setFrom(emailRemitente);
            helper.addAttachment(nombreAdjunto,
                    new org.springframework.core.io.ByteArrayResource(adjunto));
            mailSender.send(message);
            log.info("Email con adjunto enviado a: {}", to);
        } catch (Exception e) {
            log.error("Error enviando email con adjunto a {}: {}", to, e.getMessage());
            throw new RuntimeException("Error enviando email con adjunto", e);
        }
    }
}