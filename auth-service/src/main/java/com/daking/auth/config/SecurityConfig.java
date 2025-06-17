package com.daking.auth.config;

import lombok.RequiredArgsConstructor;
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

        @Bean
        public JwtDecoder jwtDecoder() {
                String secretKey = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
                byte[] keyBytes = secretKey.getBytes();
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
                configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173"));
                configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                configuration.setAllowedHeaders(List.of("*"));
                configuration.setAllowCredentials(true);

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
                                response.sendRedirect("http://localhost:5173/auth/oauth-success?error=missing_email");
                                return;
                        }
                        // Generate JWT and refresh token for the user
                        var user = userService.getUserByEmail(email).orElseThrow();
                        String accessToken = userService.generateTokenForOAuthUser(email);
                        String refreshToken = userService.getJwtService().generateRefreshToken(user);
                        // Redirect to frontend with both tokens as query params
                        String redirectUrl = "http://localhost:5173/auth/oauth-success?token=" + accessToken
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