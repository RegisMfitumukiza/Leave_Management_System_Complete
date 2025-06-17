package com.daking.auth.config;

import com.daking.auth.service.JWTService;
import com.daking.auth.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.GrantedAuthority;

import java.io.IOException;
import java.util.Collection;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JWTService jwtService;
    private final UserService userService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        System.out.println("[JwtAuthenticationFilter] Raw Authorization header: " + authHeader);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            jwt = authHeader.substring(7).trim();
            System.out.println("[JwtAuthenticationFilter] JWT value: '" + jwt + "'");
            if (jwt.isEmpty()) {
                System.err.println("[JwtAuthenticationFilter] JWT is empty after trimming. Skipping authentication.");
                filterChain.doFilter(request, response);
                return;
            }
        username = jwtService.extractUsername(jwt);

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userService.loadUserByUsername(username);

            if (userDetails != null && jwtService.isTokenValid(jwt, userDetails)) {
                    // Extract authorities from the token
                    Collection<? extends GrantedAuthority> authorities = jwtService.extractAuthorities(jwt);

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                            authorities != null ? authorities : userDetails.getAuthorities());

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);

                    System.out.println("[JwtAuthenticationFilter] User authenticated: " + username);
                    System.out.println("[JwtAuthenticationFilter] Authorities: " + authToken.getAuthorities());
                }
            }
        } catch (Exception e) {
            System.err.println("[JwtAuthenticationFilter] Error processing JWT: " + e.getMessage());
            SecurityContextHolder.clearContext();
            }

        filterChain.doFilter(request, response);
    }
}