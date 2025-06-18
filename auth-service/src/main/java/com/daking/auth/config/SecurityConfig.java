package com.daking.auth.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import javax.crypto.SecretKey;
import io.jsonwebtoken.security.Keys;
import com.daking.auth.service.UserService;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthFilter;
        private final AuthenticationProvider authenticationProvider;
        private final UserService userService;

        @Value("${jwt.secret}")
        private String jwtSecret;

        @Value("${app.cors.allowed-origins}")
        private String allowedOrigins;

        @Value("${app.cors.allowed-methods}")
        private String allowedMethods;

        @Value("${app.cors.allowed-headers}")
        private String allowedHeaders;

        @Value("${app.cors.allow-credentials}")
        private boolean allowCredentials;

        @Value("${app.cors.max-age}")
        private long maxAge;

        @Bean
        public JwtDecoder jwtDecoder() {
                byte[] keyBytes = jwtSecret.getBytes();
                SecretKey key = Keys.hmacShaKeyFor(keyBytes);
                return NimbusJwtDecoder.withSecretKey(key).build();
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .csrf(AbstractHttpConfigurer::disable)
                                .authorizeHttpRequests(auth -> auth
                                                // Public endpoints
                                                .requestMatchers(
                                                                "/api/auth/**",
                                                                "/api/oauth2/**",
                                                                "/oauth2/**",
                                                                "/login/oauth2/**",
                                                                "/actuator/health",
                                                                "/actuator/info",
                                                                // Swagger/OpenAPI
                                                                "/v2/api-docs",
                                                                "/v3/api-docs",
                                                                "/v3/api-docs/**",
                                                                "/swagger-resources",
                                                                "/swagger-resources/**",
                                                                "/configuration/ui",
                                                                "/configuration/security",
                                                                "/swagger-ui/**",
                                                                "/webjars/**",
                                                                "/swagger-ui.html")
                                                .permitAll()
                                                // Role-based endpoints
                                                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                                                .requestMatchers("/api/manager/**").hasRole("MANAGER")
                                                // Require authentication for any other request
                                                .anyRequest().authenticated())
                                .exceptionHandling(exception -> exception
                                                .authenticationEntryPoint(
                                                                new org.springframework.security.web.authentication.HttpStatusEntryPoint(
                                                                                org.springframework.http.HttpStatus.UNAUTHORIZED)))
                                .oauth2Login(oauth2 -> oauth2
                                                .authorizationEndpoint(authorization -> authorization
                                                                .baseUri("/oauth2/authorization"))
                                                .redirectionEndpoint(redirection -> redirection
                                                                .baseUri("/oauth2/authorization/google/callback"))
                                                .successHandler(oAuth2AuthenticationSuccessHandler()))
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                                .authenticationProvider(authenticationProvider)
                                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                                .oauth2ResourceServer(oauth2 -> oauth2
                                                .jwt(jwt -> jwt.jwtAuthenticationConverter(
                                                                jwtAuthenticationConverter())));

                return http.build();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();

                // Parse allowed origins (comma-separated string)
                String[] origins = allowedOrigins.split(",");
                configuration.setAllowedOrigins(Arrays.asList(origins));

                // Parse allowed methods (comma-separated string)
                String[] methods = allowedMethods.split(",");
                configuration.setAllowedMethods(Arrays.asList(methods));

                // Parse allowed headers
                if ("*".equals(allowedHeaders)) {
                configuration.setAllowedHeaders(List.of("*"));
                } else {
                        String[] headers = allowedHeaders.split(",");
                        configuration.setAllowedHeaders(Arrays.asList(headers));
                }

                configuration.setAllowCredentials(allowCredentials);
                configuration.setMaxAge(maxAge);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }

        @Bean
        public AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler() {
                return (request, response, authentication) -> {
                        // Extract user email from authentication
                        String email = null;
                        if (authentication
                                        .getPrincipal() instanceof org.springframework.security.oauth2.core.user.OAuth2User) {
                                email = ((org.springframework.security.oauth2.core.user.OAuth2User) authentication
                                                .getPrincipal()).getAttribute("email");
                        }
                        if (email == null) {
                                // Use the first allowed origin as the frontend URL
                                String frontendUrl = allowedOrigins.split(",")[0];
                                response.sendRedirect(frontendUrl + "/auth/oauth-success?error=missing_email");
                                return;
                        }
                        // Generate JWT and refresh token for the user
                        var user = userService.getUserByEmail(email).orElseThrow();
                        String accessToken = userService.generateTokenForOAuthUser(email);
                        String refreshToken = userService.getJwtService().generateRefreshToken(user);

                        // Use the first allowed origin as the frontend URL
                        String frontendUrl = allowedOrigins.split(",")[0];
                        String redirectUrl = frontendUrl + "/auth/oauth-success?token=" + accessToken
                                        + "&refreshToken=" + refreshToken;
                        RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();
                        redirectStrategy.sendRedirect(request, response, redirectUrl);
                };
        }

        @Bean
        public JwtAuthenticationConverter jwtAuthenticationConverter() {
                JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
                converter.setJwtGrantedAuthoritiesConverter(jwt -> {
                        List<String> roles = jwt.getClaimAsStringList("roles");
                        if (roles == null)
                                return List.of();
                        return roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
                });
                return converter;
        }
}