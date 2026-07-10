package com.example.Notification_Service.Model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notificaciones")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String usuarioId;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String mensaje;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoNotificacion tipo;

    @Column(nullable = false)
    @Builder.Default
    private Boolean leido = false;

    @Column(length = 100)
    private String eventoId;

    @Column(length = 200)
    private String eventoNombre;

    @Column(length = 100)
    private String emailDestino;

    @Column(nullable = false)
    @Builder.Default
    private Boolean emailEnviado = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    //telefono de destino
    @Column(length = 20)
    private String telefonoDestino;


    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}