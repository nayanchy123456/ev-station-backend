package com.evstation.ev_charging_backend.serviceImpl;

import com.evstation.ev_charging_backend.dto.AuthResponse;
import com.evstation.ev_charging_backend.dto.LoginRequest;
import com.evstation.ev_charging_backend.dto.RegisterRequest;
import com.evstation.ev_charging_backend.entity.User;
import com.evstation.ev_charging_backend.enums.Role;
import com.evstation.ev_charging_backend.exception.InvalidCredentialsException;
import com.evstation.ev_charging_backend.exception.InvalidPhoneNumberException;
import com.evstation.ev_charging_backend.exception.UserAlreadyExistsException;
import com.evstation.ev_charging_backend.repository.UserRepository;
import com.evstation.ev_charging_backend.security.CustomUserDetails;
import com.evstation.ev_charging_backend.security.JwtUtil;
import com.evstation.ev_charging_backend.service.UserService;
import com.evstation.ev_charging_backend.util.PhoneValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @Override
    public AuthResponse register(RegisterRequest request) {
        // Validate phone number for Nepal format
        if (!PhoneValidator.isValidNepalPhone(request.getPhone())) {
            throw new InvalidPhoneNumberException(PhoneValidator.getErrorMessage());
        }

        // Check if email or phone already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email is already registered!");
        }
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new UserAlreadyExistsException("Phone number is already registered!");
        }

        // Prevent admin registration
        if (request.getRole() == Role.ADMIN) {
            throw new RuntimeException("Cannot register as admin");
        }

        // Assign role: HOST → PENDING_HOST, others → USER
        Role assignedRole = (request.getRole() == Role.HOST) ? Role.PENDING_HOST : Role.USER;

        // Clean and format phone number with country code
        String formattedPhone = PhoneValidator.cleanPhoneNumber(request.getPhone());

        // Build user entity
        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(formattedPhone)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(assignedRole)
                .build();

        // Save user
        User savedUser = userRepository.save(user);

        // ✅ UPDATED: Generate token WITH userId only for non-PENDING_HOST users
        String token = null;
        if (assignedRole != Role.PENDING_HOST) {
            token = jwtUtil.generateToken(
                savedUser.getEmail(), 
                assignedRole.name(),
                savedUser.getUserId()  // ← ADDED THIS
            );
        }

        // Build response
        return AuthResponse.builder()
                .message(assignedRole == Role.PENDING_HOST ?
                        "Host registration pending approval by admin" :
                        "User registered successfully!")
                .token(token)
                .email(savedUser.getEmail())
                .role(assignedRole.name())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .phone(savedUser.getPhone())
                .createdAt(savedUser.getCreatedAt())
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        } catch (Exception e) {
            throw new InvalidCredentialsException("Invalid email or password!");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password!"));

        // ✅ UPDATED: Generate token WITH userId only for non-PENDING_HOST users
        String token = null;
        if (user.getRole() != Role.PENDING_HOST) {
            token = jwtUtil.generateToken(
                user.getEmail(), 
                user.getRole().name(),
                user.getUserId()  // ← ADDED THIS
            );
        }

        return AuthResponse.builder()
                .message(user.getRole() == Role.PENDING_HOST ?
                        "Host registration pending approval" : "Login successful!")
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