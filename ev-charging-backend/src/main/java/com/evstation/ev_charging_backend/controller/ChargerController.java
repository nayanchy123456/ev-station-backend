package com.evstation.ev_charging_backend.controller;

import com.evstation.ev_charging_backend.dto.ChargerRequestDto;
import com.evstation.ev_charging_backend.dto.ChargerResponseDto;
import com.evstation.ev_charging_backend.entity.User;
import com.evstation.ev_charging_backend.repository.UserRepository;
import com.evstation.ev_charging_backend.security.JwtUtil;
import com.evstation.ev_charging_backend.service.ChargerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chargers")
public class ChargerController {

    private final ChargerService chargerService;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public ChargerController(ChargerService chargerService, UserRepository userRepository, JwtUtil jwtUtil) {
        this.chargerService = chargerService;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    // Create a new charger (Host only)
    @PostMapping
    public ResponseEntity<ChargerResponseDto> createCharger(
            @Valid @RequestBody ChargerRequestDto dto,
            @AuthenticationPrincipal UserDetails userDetails) {

        User host = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Host not found"));

        ChargerResponseDto response = chargerService.createCharger(dto, host.getUserId());
        return ResponseEntity.ok(response);
    }

    // Update charger (Host can update their own, Admin can update all)
    @PutMapping("/{id}")
    public ResponseEntity<ChargerResponseDto> updateCharger(
            @PathVariable Long id,
            @Valid @RequestBody ChargerRequestDto dto,
            @AuthenticationPrincipal UserDetails userDetails) {

        User currentUser = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        ChargerResponseDto response = chargerService.updateCharger(
                id,
                dto,
                currentUser.getUserId(),
                currentUser.getRole().name()
        );
        return ResponseEntity.ok(response);
    }

    // Delete charger (Host can delete their own, Admin can delete all)
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteCharger(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        User currentUser = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        chargerService.deleteCharger(
                id,
                currentUser.getUserId(),
                currentUser.getRole().name()
        );

        Map<String, String> response = new HashMap<>();
        response.put("message", "Charger with ID " + id + " has been successfully deleted.");
        return ResponseEntity.ok(response);
    }

    // Get chargers
  @GetMapping
    public ResponseEntity<List<ChargerResponseDto>> getAllChargers(@AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<ChargerResponseDto> chargers;

        if ("HOST".equals(currentUser.getRole().name())) {
            // Host sees only their chargers
            chargers = chargerService.getChargersByHost(currentUser.getUserId());
        } else if ("ADMIN".equals(currentUser.getRole().name())) {
            // Admin sees all chargers
            chargers = chargerService.getAllChargers();
        } else {
            // Regular users see all chargers too
            chargers = chargerService.getAllChargers();
        }

        return ResponseEntity.ok(chargers);
    }
}
