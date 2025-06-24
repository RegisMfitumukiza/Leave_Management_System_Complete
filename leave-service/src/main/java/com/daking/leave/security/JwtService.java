package com.daking.leave.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token);
            return true;
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String extractUserId(String token) {
        try {
            Claims claims = Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token).getBody();
            // Consistently read the 'userId' claim which is set as a Long by auth-service
            return claims.get("userId", Long.class).toString();
        } catch (Exception e) {
            return null;
        }
    }

    public List<String> extractRoles(String token) {
        try {
            final Claims claims = Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token)
                    .getBody();
            final Object rolesObj = claims.get("roles");

            if (rolesObj instanceof List<?>) {
                // Ensure all elements are safely converted to String to avoid runtime errors.
                return ((List<?>) rolesObj).stream()
                        .map(String::valueOf)
                        .collect(Collectors.toList());
            }
            return Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public Date extractExpiration(String token) {
        try {
            Claims claims = Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token).getBody();
            return claims.getExpiration();
        } catch (Exception e) {
            return null;
        }
    }

    private Key getSigningKey() {
        // For HS256, key should be at least 256 bits (32 bytes)
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }
}