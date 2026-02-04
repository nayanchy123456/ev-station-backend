package com.evstation.ev_charging_backend.config;

import com.evstation.ev_charging_backend.security.JwtAuthenticationFilter;
import com.evstation.ev_charging_backend.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration for the EV Charging Station Backend.
 * 
 * ⭐ UPDATED FOR PHASE 1 CHAT SYSTEM:
 * - Added WebSocket endpoint (/ws/**) - permitAll (JWT validated in interceptor)
 * - Added Chat API endpoints (/api/chat/**) - authenticated
 * - Added health endpoint (/api/chat/health) - permitAll for monitoring
 * - CORS handled by CorsFilter class (not Spring Security's cors())
 * 
 * ALL EXISTING FUNCTIONALITY PRESERVED - NO CHANGES TO:
 * - Authentication endpoints
 * - Admin endpoints
 * - File uploads
 * - All other API endpoints
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService userDetailsService;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            // ❌ REMOVED: .cors(cors -> {})  
            // CORS is now handled by CorsFilter class which runs before Spring Security
            .authorizeHttpRequests(auth -> auth

                // ========== EXISTING - UNCHANGED ==========
                
                // ✅ Allow uploads folder (images) - EXISTING
                .requestMatchers("/uploads/**").permitAll()

                // ✅ Auth endpoints open - EXISTING
                .requestMatchers("/api/auth/**").permitAll()

                // ========== PHASE 1 CHAT SYSTEM - NEW ==========
                
                // ⭐ WebSocket endpoint - permitAll because JWT validation happens
                //    in WebSocketAuthInterceptor during STOMP CONNECT frame
                //    This is the standard Spring Security pattern for WebSocket + JWT
                .requestMatchers("/ws/**").permitAll()

                // ⭐ Health check endpoint - permitAll for monitoring/debugging
                //    This allows testing if backend is running without authentication
                .requestMatchers("/api/chat/health").permitAll()

                // ⭐ Chat REST API - requires authentication via JWT filter
                //    All other chat endpoints need valid JWT token in Authorization header
                .requestMatchers("/api/chat/**").authenticated()

                // ========== EXISTING - UNCHANGED ==========

                // ✅ Admin endpoints - EXISTING
                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                // ✅ Everything else needs login - EXISTING
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            );

        // Add JWT filter - UNCHANGED
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}