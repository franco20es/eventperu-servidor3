package com.example.Notification_Service.Service.Implements;

import com.example.Notification_Service.Model.PreferenciasModel;

import com.example.Notification_Service.Repository.PreferenciasRepository;
import com.example.Notification_Service.dto.Request.PreferenciasRequest;
import com.example.Notification_Service.dto.Response.PreferenciasResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PreferenciasService {

    private final PreferenciasRepository repo;

    // Obtener — si no existe, devuelve defaults
    @Transactional(readOnly = true)
    public PreferenciasResponse obtener(String usuarioId) {
        PreferenciasModel prefs = repo.findById(usuarioId)
                .orElse(PreferenciasModel.builder()
                        .usuarioId(usuarioId)
                        .emailActivo(true)
                        .pushActivo(true)
                        .smsActivo(false)
                        .promocionesActivo(true)
                        .build());
        return toResponse(prefs);
    }

    // Guardar o actualizar
    @Transactional
    public PreferenciasResponse guardar(String usuarioId, PreferenciasRequest request) {
        PreferenciasModel prefs = repo.findById(usuarioId)
                .orElse(PreferenciasModel.builder().usuarioId(usuarioId).build());

        prefs.setEmailActivo(request.isEmailActivo());
        prefs.setPushActivo(request.isPushActivo());
        prefs.setSmsActivo(request.isSmsActivo());
        prefs.setPromocionesActivo(request.isPromocionesActivo());

        repo.save(prefs);
        log.info("Preferencias guardadas para usuario: {}", usuarioId);
        return toResponse(prefs);
    }

    // Consultar si debe enviar email (usado en NotificationServiceImpl)
    public boolean debeEnviarEmail(String usuarioId) {
        return repo.findById(usuarioId)
                .map(PreferenciasModel::isEmailActivo)
                .orElse(true); // default: sí
    }

    public boolean debeEnviarSms(String usuarioId) {
        return repo.findById(usuarioId)
                .map(PreferenciasModel::isSmsActivo)
                .orElse(false);
    }

    private PreferenciasResponse toResponse(PreferenciasModel p) {
        return PreferenciasResponse.builder()
                .usuarioId(p.getUsuarioId())
                .emailActivo(p.isEmailActivo())
                .pushActivo(p.isPushActivo())
                .smsActivo(p.isSmsActivo())
                .promocionesActivo(p.isPromocionesActivo())
                .build();
    }
}