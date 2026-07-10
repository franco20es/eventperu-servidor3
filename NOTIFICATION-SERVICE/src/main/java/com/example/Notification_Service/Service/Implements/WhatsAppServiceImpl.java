package com.example.Notification_Service.Service.Implements;

import com.example.Notification_Service.Service.WhatsAppService;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class WhatsAppServiceImpl implements WhatsAppService {

    @Override
    public void enviarMensaje(String numero, String mensaje) {

        String url = "http://localhost:3000/send";

        RestTemplate restTemplate = new RestTemplate();

        Map<String, String> body = new HashMap<>();

        body.put("number", numero);
        body.put("message", mensaje);

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> request =
                new HttpEntity<>(body, headers);

        restTemplate.postForEntity(url, request, String.class);
    }
}