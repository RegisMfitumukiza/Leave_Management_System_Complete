package com.daking.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@Configuration
@EnableWebSecurity
@ConditionalOnProperty(name = "spring.security.oauth2.client.registration.google.client-id", havingValue = ".+", matchIfMissing = false)
public class OAuth2Config {
    
    @Value("${spring.security.oauth2.client.registration.google.client-id:}")
    private String googleClientId;
    
    @Value("${spring.security.oauth2.client.registration.google.client-secret:}")
    private String googleClientSecret;
} 