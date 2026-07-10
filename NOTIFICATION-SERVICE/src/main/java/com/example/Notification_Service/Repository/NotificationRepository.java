package com.example.Notification_Service.Repository;

import com.example.Notification_Service.Model.NotificationModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<NotificationModel, String> {

    Page<NotificationModel> findByUsuarioIdOrderByCreatedAtDesc(String usuarioId, Pageable pageable);

    List<NotificationModel> findByUsuarioIdAndLeidoFalseOrderByCreatedAtDesc(String usuarioId);

    long countByUsuarioIdAndLeidoFalse(String usuarioId);

}
