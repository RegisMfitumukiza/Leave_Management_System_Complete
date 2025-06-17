package com.daking.leave.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.util.Map;

@Service
public class ServiceAccountTokenProvider {

    @Value("${service.account.email}")
    private String serviceAccountEmail;

    @Value("${service.account.password}")
    private String serviceAccountPassword;

    @Value("${auth.service.url}")
    private String authServiceUrl; // e.g., http://auth-service:8080

    private String cachedToken;
    private long tokenExpiry = 0;

    public synchronized String getToken() {
        long now = System.currentTimeMillis();
        if (cachedToken == null || now > tokenExpiry) {
            fetchToken();
        }
        return cachedToken;
    }

    @SuppressWarnings("unchecked")
    private void fetchToken() {
        RestTemplate restTemplate = new RestTemplate();
        String url = authServiceUrl + "/api/auth/login";
        Map<String, String> request = Map.of(
                "email", serviceAccountEmail,
                "password", serviceAccountPassword);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);

        ResponseEntity<Map<String, Object>> response = restTemplate.postForEntity(url, entity,
                (Class<Map<String, Object>>) (Class<?>) Map.class);
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            cachedToken = "Bearer " + response.getBody().get("accessToken");
            // Optionally parse expiry from JWT or set a fixed refresh interval
            tokenExpiry = System.currentTimeMillis() + 1000 * 60 * 50; // 50 minutes
        } else {
            throw new RuntimeException("Failed to fetch service account token");
        }
    }
}