package com.daking.leave.client;

import com.daking.auth.api.dto.LoginRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "auth-service-internal", url = "${auth.service.url}")
public interface AuthClient {

    @PostMapping("/api/auth/login")
    ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest loginRequest);

}