package com.example.Notification_Service.client;


import com.example.Notification_Service.dto.Response.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "user-service")
public interface UserClient {
        @GetMapping("/api/v1/users/activos")
        List<UserResponse> getAllUsers();


}
