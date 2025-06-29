package com.daking.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import com.daking.auth.model.Role;
import com.daking.auth.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Key;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class JWTService {

    private static final Logger logger = LoggerFactory.getLogger(JWTService.class);

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshExpiration;

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        String roleName = null;
        Long userId = null;
        if (userDetails instanceof com.daking.auth.model.UserPrincipal) {
            var up = (com.daking.auth.model.UserPrincipal) userDetails;
            roleName = up.getRole().name();
            userId = up.getId();
        } else if (userDetails instanceof User) {
            User user = (User) userDetails;
            roleName = user.getRole().name();
            userId = user.getId();
        }
        claims.put("role", roleName);
        claims.put("roles", List.of("ROLE_" + roleName));
        claims.put("userId", userId); // Numeric ID
        // ... other claims as needed
        return generateToken(claims, userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return buildToken(extraClaims, userDetails, jwtExpiration);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken(new HashMap<>(), userDetails, refreshExpiration);
    }

    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiration) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(getUserId(userDetails))
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    public boolean hasRole(String token, Role role) {
        try {
            String tokenRole = extractClaim(token, claims -> claims.get("role", String.class));
            return role.name().equals(tokenRole);
        } catch (Exception e) {
            return false;
        }
    }

    public Long extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", Long.class));
    }

    public Long extractDepartmentId(String token) {
        return extractClaim(token, claims -> claims.get("departmentId", Long.class));
    }

    @SuppressWarnings("unchecked")
    public Collection<? extends GrantedAuthority> extractAuthorities(String token) {
        try {
            Claims claims = extractAllClaims(token);
            List<String> roles = claims.get("roles", List.class);
            if (roles != null) {
                return roles.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
            }
            // Fallback to role claim if roles list is not present
            String role = claims.get("role", String.class);
            if (role != null) {
                return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role));
            }
        } catch (Exception e) {
            logger.error("[JWTService] Error extracting authorities: {}", e.getMessage());
        }
        return null;
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignInKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    private String getUserId(UserDetails userDetails) {
        return userDetails.getUsername();
    }
}