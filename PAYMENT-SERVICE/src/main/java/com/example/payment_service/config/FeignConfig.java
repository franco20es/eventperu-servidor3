package com.example.payment_service.config;


import feign.RequestInterceptor;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.codec.ErrorDecoder;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
@Slf4j
public class FeignConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return template -> {
            template.header("Content-Type", "application/json");
            log.info("=== REQUEST FEIGN ===");
            log.info("Method: {}", template.method());
            log.info("URL: {}", template.url());
            log.info("Headers: {}", template.headers());
            if (template.body() != null) {
                try {
                    log.info("Body: {}", new String(template.body()));
                } catch (Exception e) {
                    log.info("Body: [binary data]");
                }
            }
        };
    }

    @Bean
    public Decoder decoder() {
        return new Decoder() {
            private final JacksonDecoder jacksonDecoder = new JacksonDecoder();

            @Override
            public Object decode(feign.Response response, java.lang.reflect.Type type) throws IOException {
                // Leer el body como String para loguearlo
                String body = null;
                if (response.body() != null) {
                    body = new String(response.body().asInputStream().readAllBytes(), StandardCharsets.UTF_8);
                    log.info("=== RESPUESTA CRUDA DE CULQI ===");
                    log.info("Status: {}", response.status());
                    log.info("Body: {}", body);
                }

                // Crear una nueva respuesta con el body leído
                feign.Response newResponse = response.toBuilder()
                        .body(body, StandardCharsets.UTF_8)
                        .build();

                try {
                    Object result = jacksonDecoder.decode(newResponse, type);
                    log.info("=== RESPUESTA PARSEADA ===");
                    log.info("Result: {}", result);
                    return result;
                } catch (Exception e) {
                    log.error("Error al parsear respuesta: {}", e.getMessage());
                    log.error("Body que causó error: {}", body);
                    throw e;
                }
            }
        };
    }

    @Bean
    public Encoder encoder() {
        return new JacksonEncoder();
    }

    @Bean
    public ErrorDecoder errorDecoder() {
        return (methodKey, response) -> {
            log.error("=== ERROR DECODER FEIGN ===");
            log.error("Status: {}", response.status());
            log.error("Reason: {}", response.reason());
            try {
                if (response.body() != null) {
                    String body = new String(response.body().asInputStream().readAllBytes());
                    log.error("Body: {}", body);
                    if (body.contains("user_message")) {
                        int start = body.indexOf("user_message") + 14;
                        int end = body.indexOf("\"", start);
                        if (start > 14 && end > start) {
                            return new RuntimeException("Culqi error: " + body.substring(start, end));
                        }
                    }
                    return new RuntimeException("Error en API Culqi: " + body);
                }
            } catch (Exception e) {
                log.error("Error leyendo body", e);
            }
            return new RuntimeException("Error en API Culqi: status=" + response.status());
        };
    }
}