package com.example.Notification_Service.dto.Request;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreferenciasRequest {
    private boolean emailActivo;
    private boolean pushActivo;
    private boolean smsActivo;
    private boolean promocionesActivo;
}
