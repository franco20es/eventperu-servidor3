package com.example.Notification_Service;

import com.example.Notification_Service.Controller.NotificationController;
// import com.example.Notification_Service.Model.Status; // Verifica si 'Status' existe realmente
import com.example.Notification_Service.Service.NotificationService;
import com.example.Notification_Service.dto.Request.NotificationRequest;
import com.example.Notification_Service.dto.Response.NotificationResponse; // Corregido el nombre
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus; // Usar HttpStatus es más común
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NotificationControllerTests {

    @Test
    public void send_shouldReturnOkAndBodyFromService() {
        // 1. Mock del servicio
        NotificationService service = mock(NotificationService.class);
        NotificationController controller = new NotificationController(service);

        // 2. Construcción del Request (Usa Builder si lo tienes en el DTO)
        NotificationRequest request = new NotificationRequest();
        // Corregido: antes decía .e("..."), debe ser el nombre real del campo, ej: setEmailDestino
        request.setEmailDestino("test@example.com");
        request.setTitulo("Prueba");
        request.setMensaje("Hola");

        // 3. Construcción del Response (Corregido 'NotificacionResponse' -> 'NotificationResponse')
        NotificationResponse response = NotificationResponse.builder()
                .titulo("Prueba")
                .mensaje("Correo enviado correctamente")
                .id("1") // En tu DTO el ID es String
                .build();

        // 4. Comportamiento del Mock
        // Asegúrate de que el método en el service se llame 'crearNotificacion' o como lo hayas definido
        when(service.crearNotificacion(request)).thenReturn(response);

        // 5. Ejecución (Corregido: controller.nombreDelMetodo)
        // Según tu image_bea1f0.png, tienes métodos como 'porUsuario', verifica el de enviar
        ResponseEntity<NotificationResponse> result = controller.crear(request);

        // 6. Verificaciones
        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(response, result.getBody());
    }
}