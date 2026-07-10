package com.example.payment_service.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        // ── Públicos ───────────────────────────────────────────────
                        .requestMatchers("/health", "/ready").permitAll()
                        .requestMatchers("/api/v1/payments/webhook").permitAll()
                        .requestMatchers("/api/v1/webhooks/**").permitAll()
                        .requestMatchers("/pago/exitoso", "/pago/fallido",
                                "/pago/pendiente").permitAll()

                        // check QR — público (página /ticket/verify del frontend)
                        .requestMatchers("/api/v1/tickets/check").permitAll()

                        // ── Validar ticket — ROLE_STAFF o ROLE_ADMIN ──────────────
                        .requestMatchers("/api/v1/tickets/validate")
                        .hasAnyAuthority("ROLE_STAFF", "ROLE_ADMIN")

                        // ── Tickets del usuario ────────────────────────────────────
                        .requestMatchers("/api/v1/tickets/user/**")
                        .hasAnyAuthority("ROLE_USER", "ROLE_ADMIN")
                        .requestMatchers("/api/v1/tickets/*/pdf")
                        .hasAnyAuthority("ROLE_USER", "ROLE_ADMIN")
                        .requestMatchers("/api/v1/tickets/**")
                        .hasAnyAuthority("ROLE_USER", "ROLE_ADMIN", "ROLE_STAFF")

                        // ── Órdenes ────────────────────────────────────────────────
                        .requestMatchers("/api/v1/orders/user/**")
                        .hasAnyAuthority("ROLE_USER", "ROLE_ADMIN")
                        .requestMatchers("/api/v1/orders/**")
                        .hasAnyAuthority("ROLE_USER", "ROLE_ADMIN")

                        // ── Pagos ──────────────────────────────────────────────────
                        .requestMatchers("/api/v1/payments/create")
                        .hasAnyAuthority("ROLE_USER", "ROLE_ADMIN")
                        .requestMatchers("/api/v1/payments/comprobantes/**")
                        .hasAnyAuthority("ROLE_USER", "ROLE_ADMIN")
                        .requestMatchers("/api/v1/payments/**")
                        .hasAnyAuthority("ROLE_USER", "ROLE_ADMIN")
                        .requestMatchers("/api/v1/payments/confirm/**")
                        .hasAnyAuthority("ROLE_USER", "ROLE_ADMIN")
                        .anyRequest().authenticated()
                )
                // ← Filtro JWT activado
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
       config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setExposedHeaders(List.of("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
