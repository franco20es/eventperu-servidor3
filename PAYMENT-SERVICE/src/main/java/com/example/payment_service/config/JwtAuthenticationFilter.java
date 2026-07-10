package com.example.payment_service.config;

import com.example.payment_service.Util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String authHeader = request.getHeader("Authorization");

        // 1. Si no hay Header de autorización o no empieza con Bearer,
        // simplemente dejamos pasar la petición al siguiente filtro/configuración.
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Si hay un token, intentamos validarlo
        String token = authHeader.substring(7);

        try {
            if (jwtUtil.isTokenValid(token)) {
                String userId = jwtUtil.extractUserId(token);
                String roles = jwtUtil.extractRoles(token);

                List<SimpleGrantedAuthority> authorities = List.of();
                if (roles != null) {
                    authorities = List.of(roles.split(",")).stream()
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());
                }

                UsernamePasswordAuthenticationToken authentication
                        = new UsernamePasswordAuthenticationToken(userId, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("Token válido - Usuario: {}", userId);
            } else {

                log.warn("Token inválido o expirado");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Token inválido o expirado");
                return;
            }
        } catch (Exception e) {
            log.error("Error procesando el token: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // 3. Continuar con la cadena de filtros
        filterChain.doFilter(request, response);
    }
}
