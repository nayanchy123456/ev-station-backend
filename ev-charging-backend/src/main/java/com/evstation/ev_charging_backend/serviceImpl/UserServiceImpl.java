package com.evstation.ev_charging_backend.serviceImpl;

import com.evstation.ev_charging_backend.dto.AuthResponse;
import com.evstation.ev_charging_backend.dto.LoginRequest;
import com.evstation.ev_charging_backend.dto.RegisterRequest;
import com.evstation.ev_charging_backend.entity.User;
import com.evstation.ev_charging_backend.exception.InvalidCredentialsException;
import com.evstation.ev_charging_backend.exception.UserAlreadyExistsException;
import com.evstation.ev_charging_backend.repository.UserRepository;
import com.evstation.ev_charging_backend.security.CustomUserDetails;
import com.evstation.ev_charging_backend.security.JwtUtil;
import com.evstation.ev_charging_backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

 @Override
public AuthResponse register(RegisterRequest request) {
    boolean emailExists = userRepository.existsByEmail(request.getEmail());
    boolean phoneExists = userRepository.existsByPhone(request.getPhone());

    if (emailExists && phoneExists) {
        throw new UserAlreadyExistsException("Email and Phone are already registered!");
    } else if (emailExists) {
        throw new UserAlreadyExistsException("Email is already registered!");
    } else if (phoneExists) {
        throw new UserAlreadyExistsException("Phone number is already registered!");
    }

    User user = User.builder()
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .email(request.getEmail())
            .phone(request.getPhone())
            .password(passwordEncoder.encode(request.getPassword()))
            .role(request.getRole())
            .build();

    userRepository.save(user);

     return AuthResponse.builder()
        .message("User registered successfully!")
        .email(user.getEmail())
        .role(user.getRole().name())
        .firstName(user.getFirstName())
        .lastName(user.getLastName())
        .phone(user.getPhone())
        .createdAt(user.getCreatedAt()) // Keep it as LocalDateTime
        .build();

}


   @Override
public AuthResponse login(LoginRequest request) {
    // Authenticate credentials
    try {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
    } catch (Exception e) {
        throw new InvalidCredentialsException("Invalid email or password!");
    }

    // Load user
    User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password!"));

    // Generate JWT token
    String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

    return AuthResponse.builder()
            .message("Login successful!")
            .token(token)
            .email(user.getEmail())
            .role(user.getRole().name())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .phone(user.getPhone())
            .createdAt(user.getCreatedAt())
            .build();
}


    @Override
    public AuthResponse getCurrentUserProfile() {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof CustomUserDetails userDetails) {
                User user = userRepository.findByEmail(userDetails.getUsername())
                        .orElseThrow(() -> new InvalidCredentialsException("User not found"));

                return AuthResponse.builder()
                        .message("Profile fetched successfully")
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .role(user.getRole().name())
                        .createdAt(user.getCreatedAt())
                        .build();
            }
            return AuthResponse.builder()
                    .message("User not authenticated")
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch user profile: " + e.getMessage());
        }
    }
}
