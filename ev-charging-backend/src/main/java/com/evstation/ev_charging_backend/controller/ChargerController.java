package com.evstation.ev_charging_backend.controller;

import com.evstation.ev_charging_backend.dto.ChargerRequestDto;
import com.evstation.ev_charging_backend.dto.ChargerResponseDto;
import com.evstation.ev_charging_backend.entity.User;
import com.evstation.ev_charging_backend.exception.ResourceNotFoundException;
import com.evstation.ev_charging_backend.repository.UserRepository;
import com.evstation.ev_charging_backend.service.ChargerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chargers")
public class ChargerController {

    private final ChargerService chargerService;
    private final UserRepository userRepository;

    public ChargerController(ChargerService chargerService, UserRepository userRepository) {
        this.chargerService = chargerService;
        this.userRepository = userRepository;
    }

    /**
     * Create a new charger with multiple image uploads
     * Frontend should send multipart/form-data with:
     * - name, brand, location, pricePerKwh as form fields
     * - images as file array
     */
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<?> addCharger(
            @RequestParam("name") String name,
            @RequestParam("brand") String brand,
            @RequestParam("location") String location,
            @RequestParam("pricePerKwh") Double pricePerKwh,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User host = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Host not found"));

            // Validate image files if provided
            if (images != null && !images.isEmpty()) {
                for (MultipartFile image : images) {
                    if (!isValidImage(image)) {
                        return ResponseEntity.badRequest()
                                .body(Map.of("error", "Invalid image file: " + image.getOriginalFilename()));
                    }
                }
            }

            ChargerRequestDto dto = ChargerRequestDto.builder()
                    .name(name)
                    .brand(brand)
                    .location(location)
                    .pricePerKwh(java.math.BigDecimal.valueOf(pricePerKwh))
                    .build();

            ChargerResponseDto savedCharger = chargerService.addCharger(dto, host.getUserId(), images);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedCharger);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to add charger: " + e.getMessage()));
        }
    }

    /**
     * Update charger with optional new images
     * If images are provided, they will replace the existing images
     */
    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    public ResponseEntity<?> updateCharger(
            @PathVariable Long id,
            @RequestParam("name") String name,
            @RequestParam("brand") String brand,
            @RequestParam("location") String location,
            @RequestParam("pricePerKwh") Double pricePerKwh,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            @RequestParam(value = "keepExistingImages", defaultValue = "false") boolean keepExistingImages,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User currentUser = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Validate new images if provided
            if (images != null && !images.isEmpty()) {
                for (MultipartFile image : images) {
                    if (!isValidImage(image)) {
                        return ResponseEntity.badRequest()
                                .body(Map.of("error", "Invalid image file: " + image.getOriginalFilename()));
                    }
                }
            }

            ChargerRequestDto dto = ChargerRequestDto.builder()
                    .name(name)
                    .brand(brand)
                    .location(location)
                    .pricePerKwh(java.math.BigDecimal.valueOf(pricePerKwh))
                    .build();

            ChargerResponseDto updatedCharger = chargerService.updateCharger(
                    id,
                    dto,
                    currentUser.getUserId(),
                    currentUser.getRole().name(),
                    images,
                    keepExistingImages
            );

            return ResponseEntity.ok(updatedCharger);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to update charger: " + e.getMessage()));
        }
    }

    /**
     * Delete charger (Host can delete their own, Admin can delete all)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteCharger(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        User currentUser = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        chargerService.deleteCharger(
                id,
                currentUser.getUserId(),
                currentUser.getRole().name()
        );

        Map<String, String> response = new HashMap<>();
        response.put("message", "Charger with ID " + id + " has been successfully deleted.");
        return ResponseEntity.ok(response);
    }

    /**
     * Get all chargers (or host-specific chargers)
     */
    @GetMapping
    public ResponseEntity<List<ChargerResponseDto>> getAllChargers(
            @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<ChargerResponseDto> chargers;
        if ("HOST".equals(currentUser.getRole().name())) {
            chargers = chargerService.getChargersByHost(currentUser.getUserId());
        } else {
            chargers = chargerService.getAllChargers();
        }

        return ResponseEntity.ok(chargers);
    }

    /**
     * Get single charger by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getChargerById(@PathVariable Long id) {
        try {
            ChargerResponseDto charger = chargerService.getChargerById(id);
            return ResponseEntity.ok(charger);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Search chargers with optional filters
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchChargers(
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice) {

        try {
            List<ChargerResponseDto> results = chargerService.searchChargers(brand, location, minPrice, maxPrice);
            return ResponseEntity.ok(results);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "timestamp", Instant.now(),
                            "status", 500,
                            "error", "Internal Server Error",
                            "message", ex.getMessage()
                    ));
        }
    }

    /**
     * Get nearby chargers within radius
     */
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

    /**
     * Validate image file
     */
    private boolean isValidImage(MultipartFile file) {
        if (file.isEmpty()) {
            return false;
        }

        // Check file size (max 5MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            return false;
        }

        // Check content type
        String contentType = file.getContentType();
        return contentType != null && (
                contentType.equals("image/jpeg") ||
                contentType.equals("image/png") ||
                contentType.equals("image/jpg") ||
                contentType.equals("image/webp")
        );
    }
}