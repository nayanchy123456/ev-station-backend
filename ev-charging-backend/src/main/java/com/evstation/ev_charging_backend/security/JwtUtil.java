package com.evstation.ev_charging_backend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    private final Key key;
    private final long expirationTime;

    // Inject values from application.properties
    public JwtUtil(
            @Value("${jwt.secret}") String secretKey,
            @Value("${jwt.expiration}") long expirationTime
    ) {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes());
        this.expirationTime = expirationTime;
    }

    // ✅ New method: generate token with username/email, role, and numeric userId
    public String generateToken(String username, String role, Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        claims.put("userId", userId); // numeric userId for backend ownership checks

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username) // email/username
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // ⚡ Backward-compatible overload: generate token without userId
    // Keeps old frontend working
    public String generateToken(String username, String role) {
        return generateToken(username, role, null);
    }

    // Extract username/email from token
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    // Extract role from token
    public String extractRole(String token) {
        return (String) extractAllClaims(token).get("role");
    }

    // Extract numeric userId from token
    public Long extractUserId(String token) {
        Object userId = extractAllClaims(token).get("userId");
        if (userId == null) return null;
        if (userId instanceof Integer) {
            return ((Integer) userId).longValue();
        } else if (userId instanceof Long) {
            return (Long) userId;
        } else {
            throw new IllegalStateException("userId claim is missing or invalid");
        }
    }

    // Validate token by username/email and expiration
    public boolean validateToken(String token, String username) {
        return extractUsername(token).equals(username) && !isTokenExpired(token);
    }

    // Check if token is expired
    private boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    // Extract all claims from token
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
