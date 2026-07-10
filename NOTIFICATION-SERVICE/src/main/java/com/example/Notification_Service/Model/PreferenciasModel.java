package com.example.Notification_Service.Model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "preferencias_notificacion")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreferenciasModel {

    @Id
    private String usuarioId;  // 1 registro por usuario

    @Builder.Default
    private boolean emailActivo       = true;
    @Builder.Default
    private boolean pushActivo        = true;
    @Builder.Default
    private boolean smsActivo         = false;
    @Builder.Default
    private boolean promocionesActivo = true;

    private LocalDateTime updatedAt;

    @PrePersist @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
