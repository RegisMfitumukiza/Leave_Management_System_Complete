package com.daking.leave.security;

import com.daking.auth.api.dto.LoginRequest;
import com.daking.leave.client.AuthClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

@Service
public class ServiceAccountTokenProvider {

    private final String serviceAccountEmail;
    private final String serviceAccountPassword;
    private final AuthClient authClient;
    private final JwtService jwtService;

    private String cachedToken;
    private Date tokenExpiry;

    public ServiceAccountTokenProvider(
            @Value("${service.account.email}") String serviceAccountEmail,
            @Value("${service.account.password}") String serviceAccountPassword,
            AuthClient authClient,
            JwtService jwtService) {
        this.serviceAccountEmail = serviceAccountEmail;
        this.serviceAccountPassword = serviceAccountPassword;
        this.authClient = authClient;
        this.jwtService = jwtService;
    }

    public synchronized String getToken() {
        if (cachedToken == null || new Date().after(tokenExpiry)) {
            fetchAndCacheToken();
        }
        return cachedToken;
    }

    private void fetchAndCacheToken() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(serviceAccountEmail);
        loginRequest.setPassword(serviceAccountPassword);

        ResponseEntity<Map<String, Object>> response = authClient.login(loginRequest);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            String newAccessToken = (String) response.getBody().get("accessToken");
            if (newAccessToken == null) {
                throw new RuntimeException("accessToken not found in login response");
            }
            this.cachedToken = newAccessToken; // Cache the raw token

            // Extract expiry date from the new token using the dedicated service method
            this.tokenExpiry = jwtService.extractExpiration(newAccessToken);
            if (this.tokenExpiry == null) {
                throw new RuntimeException("Could not extract expiration from service account token");
            }

        } else {
            throw new RuntimeException(
                    "Failed to fetch service account token. Status: " + response.getStatusCode());
        }
    }
}