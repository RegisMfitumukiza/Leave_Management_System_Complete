package com.daking.leave.client;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;
import com.daking.leave.security.ServiceAccountTokenProvider;

@Configuration
public class UserInfoFeignConfig {
    @Bean
    public RequestInterceptor requestInterceptor(ServiceAccountTokenProvider tokenProvider) {
        return template -> {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null) {
                    template.header("Authorization", authHeader);
                }
            } else {
                // No HTTP request context, use system token
                String systemToken = tokenProvider.getToken();
                template.header("Authorization", "Bearer " + systemToken);
            }
        };
    }
}