package com.example.Notification_Service.dto.Request;

import com.example.Notification_Service.Model.TipoNotificacion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {

    @NotBlank(message = "El usuarioId es obligatorio")
    private String usuarioId;

    @NotBlank(message = "El título es obligatorio")
    private String titulo;

    private String mensaje;

    @NotBlank(message = "El tipo es obligatorio")
    private String tipo;

    private String eventoId;
    private String eventoNombre;
    private String emailDestino;

    //numero de destino
    private String telefonoDestino;
}