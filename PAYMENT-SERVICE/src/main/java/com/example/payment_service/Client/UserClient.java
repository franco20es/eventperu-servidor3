//package com.example.payment_service.Client;
//
//import com.example.payment_service.dto.Response.UsuarioResponse;
//import org.springframework.cloud.openfeign.FeignClient;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//
//@FeignClient(name = "user-service")
//public interface UserClient {
//
////    @GetMapping("/api/v1/users/perfil/{id}")
////    UsuarioResponse obtenerUsuario(@PathVariable("id") Long id);
//
//    @GetMapping("/api/v1/users/internal/{id}")
//    UsuarioResponse obtenerUsuario(@PathVariable("id") String email);
//}