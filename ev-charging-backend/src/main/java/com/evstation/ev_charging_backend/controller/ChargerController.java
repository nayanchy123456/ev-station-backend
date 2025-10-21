package com.evstation.ev_charging_backend.controller;

import com.evstation.ev_charging_backend.dto.ChargerRequestDto;
import com.evstation.ev_charging_backend.dto.ChargerResponseDto;
import com.evstation.ev_charging_backend.entity.User;
import com.evstation.ev_charging_backend.exception.ResourceNotFoundException;
import com.evstation.ev_charging_backend.repository.UserRepository;
import com.evstation.ev_charging_backend.security.JwtUtil;
import com.evstation.ev_charging_backend.service.ChargerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
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

@GetMapping("/search")
public ResponseEntity<?> searchChargers(
        @RequestParam(required = false) String brand,
        @RequestParam(required = false) String location,
        @RequestParam(required = false) Double minPrice,
        @RequestParam(required = false) Double maxPrice) {

    try {
        List<ChargerResponseDto> results = chargerService.searchChargers(brand, location, minPrice, maxPrice);
        return ResponseEntity.ok(results);
    } catch (ResourceNotFoundException ex) {
        // Return 404 with a message
        Map<String, Object> response = Map.of(
                "timestamp", Instant.now(),
                "status", 404,
                "error", "Not Found",
                "message", ex.getMessage()
        );
        return ResponseEntity.status(404).body(response);
    } catch (Exception ex) {
        // Fallback for any other errors
        Map<String, Object> response = Map.of(
                "timestamp", Instant.now(),
                "status", 500,
                "error", "Internal Server Error",
                "message", ex.getMessage()
        );
        return ResponseEntity.status(500).body(response);
    }
}




@GetMapping("/nearby")
public ResponseEntity<?> getNearbyChargers(
        @RequestParam Double userLat,
        @RequestParam Double userLng,
        @RequestParam Double radiusKm) {
    try {
        List<ChargerResponseDto> chargers = chargerService.findNearbyChargers(userLat, userLng, radiusKm);

        if (chargers.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    Map.of(
                            "timestamp", Instant.now(),
                            "status", HttpStatus.NOT_FOUND.value(),
                            "error", "Not Found",
                            "message", "No chargers found within " + radiusKm + " km radius"
                    )
            );
        }

        return ResponseEntity.ok(chargers);
    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                Map.of(
                        "timestamp", Instant.now(),
                        "status", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "error", "Internal Server Error",
                        "message", e.getMessage()
                )
        );
    }
}



}
