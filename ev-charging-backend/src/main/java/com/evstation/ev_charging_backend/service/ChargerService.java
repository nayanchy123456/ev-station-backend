package com.evstation.ev_charging_backend.service;

import com.evstation.ev_charging_backend.dto.ChargerRequestDto;
import com.evstation.ev_charging_backend.dto.ChargerResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ChargerService {

    /**
     * Add a new charger with optional images
     */
    ChargerResponseDto addCharger(ChargerRequestDto dto, Long hostId, List<MultipartFile> images) throws Exception;

    /**
     * Update an existing charger with optional images (backward compatible)
     */
    ChargerResponseDto updateCharger(Long id, ChargerRequestDto dto, Long userId, String role, 
                                    List<MultipartFile> images) throws Exception;
    
    /**
     * Update an existing charger with optional images
     * @param keepExistingImages if true, new images are appended; if false, they replace existing ones
     */
    default ChargerResponseDto updateCharger(Long id, ChargerRequestDto dto, Long userId, String role, 
                                    List<MultipartFile> images, boolean keepExistingImages) throws Exception {
        return updateCharger(id, dto, userId, role, images);
    }

    /**
     * Delete charger
     */
    void deleteCharger(Long id, Long userId, String role);

    /**
     * Get all chargers
     */
    List<ChargerResponseDto> getAllChargers();

    /**
     * Get charger by ID
     */
    ChargerResponseDto getChargerById(Long id);

    /**
     * Get chargers for a specific host
     */
    List<ChargerResponseDto> getChargersByHost(Long hostId);

    /**
     * Search chargers with optional filters
     */
    List<ChargerResponseDto> searchChargers(String brand, String location, Double minPrice, Double maxPrice);

    /**
     * Find nearby chargers within radius
     */
    List<ChargerResponseDto> findNearbyChargers(double latitude, double longitude, double radiusKm);

    /**
     * Search nearby chargers with optional filters
     */
    List<ChargerResponseDto> searchChargersNearby(
            String brand,
            String location,
            Double minPrice,
            Double maxPrice,
            Double userLat,
            Double userLng,
            Double radiusKm
    );
}