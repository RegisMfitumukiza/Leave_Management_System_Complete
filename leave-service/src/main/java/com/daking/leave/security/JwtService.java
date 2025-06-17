package com.daking.leave.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class JwtService {
    // Hardcoded secret to match auth-service
    private final String jwtSecret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    private Key getSigningKey() {
        // For HS256, key should be at least 256 bits (32 bytes)
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

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
            Object userIdClaim = claims.get("userId");
            if (userIdClaim != null) {
                return userIdClaim.toString();
            }
            return claims.getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    public List<String> extractRoles(String token) {
        try {
            Claims claims = Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token).getBody();
            Object rolesObj = claims.get("roles");
            if (rolesObj instanceof List<?> rolesList) {
                return rolesList.stream().map(Object::toString).collect(Collectors.toList());
            } else if (rolesObj instanceof String roleStr) {
                return List.of(roleStr);
            }
            // Fallback: check for 'role' (singular)
            Object roleObj = claims.get("role");
            if (roleObj instanceof String singleRole) {
                if (!singleRole.startsWith("ROLE_")) {
                    singleRole = "ROLE_" + singleRole;
                }
                return List.of(singleRole);
            }
            return Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}