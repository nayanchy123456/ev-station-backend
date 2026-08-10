package com.evstation.ev_charging_backend.serviceImpl;
import com.evstation.ev_charging_backend.dto.ChargerRequestDto;
import com.evstation.ev_charging_backend.dto.ChargerResponseDto;
import com.evstation.ev_charging_backend.entity.Charger;
import com.evstation.ev_charging_backend.entity.User;
import com.evstation.ev_charging_backend.exception.ResourceNotFoundException;
import com.evstation.ev_charging_backend.repository.ChargerRepository;
import com.evstation.ev_charging_backend.repository.UserRepository;
import com.evstation.ev_charging_backend.service.ChargerService;
import com.evstation.ev_charging_backend.service.CloudinaryService;
import com.evstation.ev_charging_backend.util.GeoCodingUtil;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChargerServiceImpl implements ChargerService {

    private final ChargerRepository chargerRepository;
    private final UserRepository userRepository;
    private final GeoCodingUtil geoCodingUtil;
    private final CloudinaryService cloudinaryService;

    public ChargerServiceImpl(ChargerRepository chargerRepository, 
                            UserRepository userRepository, 
                            GeoCodingUtil geoCodingUtil,
                            CloudinaryService cloudinaryService) {
        this.chargerRepository = chargerRepository;
        this.userRepository = userRepository;
        this.geoCodingUtil = geoCodingUtil;
        this.cloudinaryService = cloudinaryService;
    }

    @Override
    public ChargerResponseDto addCharger(ChargerRequestDto dto, Long hostId, List<MultipartFile> images) throws Exception {
        User host = userRepository.findById(hostId)
                .orElseThrow(() -> new ResourceNotFoundException("Host not found"));

        double[] latLng = geoCodingUtil.getLatLngFromAddress(dto.getLocation());
        List<String> imageUrls = saveUploadedImages(images);

        Charger charger = Charger.builder()
                .name(dto.getName())
                .brand(dto.getBrand())
                .location(dto.getLocation())
                .latitude(latLng[0])
                .longitude(latLng[1])
                .pricePerKwh(dto.getPricePerKwh())
                .images(imageUrls)
                .host(host)
                .rating(0.0)
                .build();

        chargerRepository.save(charger);
        return mapToResponse(charger);
    }

    // Original updateCharger method (backward compatible)
    @Override
    public ChargerResponseDto updateCharger(Long id, ChargerRequestDto dto, Long userId, String role, 
                                          List<MultipartFile> images) throws Exception {
        return updateCharger(id, dto, userId, role, images, false);
    }

    // New updateCharger method with keepExistingImages parameter
    @Override
    public ChargerResponseDto updateCharger(Long id, ChargerRequestDto dto, Long userId, String role, 
                                          List<MultipartFile> images, boolean keepExistingImages) throws Exception {
        Charger charger = chargerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Charger not found with ID: " + id));

        if ("HOST".equals(role) && !charger.getHost().getUserId().equals(userId)) {
            throw new AccessDeniedException("You are not the owner of this charger");
        }

       // Re-geocode if the location text changed, OR if this charger is
        // currently sitting on invalid/corrupted (0,0) coordinates from a
        // past failed geocode — this self-heals old bad data automatically.
        System.out.println("=== UPDATE CHARGER DEBUG ===");
        System.out.println("Incoming dto.location = [" + dto.getLocation() + "]");
        System.out.println("Existing charger.location = [" + charger.getLocation() + "]");
        System.out.println("Existing charger.lat/lng = " + charger.getLatitude() + ", " + charger.getLongitude());

        boolean locationChanged = !dto.getLocation().equals(charger.getLocation());
        boolean hasInvalidCoordinates = charger.getLatitude() == null || charger.getLongitude() == null
                || (charger.getLatitude() == 0.0 && charger.getLongitude() == 0.0);

        System.out.println("locationChanged = " + locationChanged);
        System.out.println("hasInvalidCoordinates = " + hasInvalidCoordinates);

        if (locationChanged || hasInvalidCoordinates) {
            System.out.println(">>> Re-geocoding triggered for: [" + dto.getLocation() + "]");
            double[] latLng = geoCodingUtil.getLatLngFromAddress(dto.getLocation());
            System.out.println(">>> Geocode result: " + latLng[0] + ", " + latLng[1]);
            if (latLng[0] != 0.0 || latLng[1] != 0.0) {
                charger.setLatitude(latLng[0]);
                charger.setLongitude(latLng[1]);
                System.out.println(">>> Coordinates UPDATED");
            } else {
                System.out.println(">>> Geocode failed (0,0) - keeping existing coordinates");
            }
        } else {
            System.out.println(">>> Skipping geocode entirely - coordinates untouched");
        }
        System.out.println("Final charger.lat/lng before save = " + charger.getLatitude() + ", " + charger.getLongitude());
        System.out.println("=============================");

        // Handle image updates

        // Handle image updates

        // Handle image updates
        if (images != null && !images.isEmpty()) {
            List<String> newImageUrls = saveUploadedImages(images);
            
            if (keepExistingImages && charger.getImages() != null) {
                // Append new images to existing ones
                List<String> allImages = new ArrayList<>(charger.getImages());
                allImages.addAll(newImageUrls);
                charger.setImages(allImages);
            } else {
                // Replace all images with new ones
                // Optionally delete old image files here
                deleteOldImages(charger.getImages());
                charger.setImages(newImageUrls);
            }
        }

        charger.setName(dto.getName());
        charger.setBrand(dto.getBrand());
        charger.setLocation(dto.getLocation());
        charger.setPricePerKwh(dto.getPricePerKwh());

        chargerRepository.save(charger);
        return mapToResponse(charger);
    }

    @Override
    public void deleteCharger(Long id, Long userId, String role) {
        Charger charger = chargerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Charger not found with ID: " + id));

        if ("HOST".equals(role) && !charger.getHost().getUserId().equals(userId)) {
            throw new AccessDeniedException("You are not the owner of this charger");
        }

        // Delete associated images
        deleteOldImages(charger.getImages());
        
        chargerRepository.delete(charger);
    }

    @Override
    public List<ChargerResponseDto> getAllChargers() {
        return chargerRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ChargerResponseDto getChargerById(Long id) {
        Charger charger = chargerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Charger not found with ID: " + id));
        return mapToResponse(charger);
    }

    @Override
    public List<ChargerResponseDto> getChargersByHost(Long hostId) {
        return chargerRepository.findByHostUserId(hostId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ChargerResponseDto> searchChargers(String brand, String location, Double minPrice, Double maxPrice) {
        return chargerRepository.findAll().stream()
                .filter(c -> brand == null || c.getBrand().toLowerCase().contains(brand.toLowerCase()))
                .filter(c -> location == null || c.getLocation().toLowerCase().contains(location.toLowerCase()))
                .filter(c -> minPrice == null || c.getPricePerKwh().doubleValue() >= minPrice)
                .filter(c -> maxPrice == null || c.getPricePerKwh().doubleValue() <= maxPrice)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ChargerResponseDto> findNearbyChargers(double latitude, double longitude, double radiusKm) {
        return chargerRepository.findAll().stream()
                .filter(c -> c.getLatitude() != null && c.getLongitude() != null)
                .filter(c -> distanceInKm(latitude, longitude, c.getLatitude(), c.getLongitude()) <= radiusKm)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ChargerResponseDto> searchChargersNearby(String brand, String location, Double minPrice, Double maxPrice,
                                                         Double userLat, Double userLng, Double radiusKm) {
        return chargerRepository.findAll().stream()
                .filter(c -> brand == null || c.getBrand().toLowerCase().contains(brand.toLowerCase()))
                .filter(c -> location == null || c.getLocation().toLowerCase().contains(location.toLowerCase()))
                .filter(c -> minPrice == null || c.getPricePerKwh().doubleValue() >= minPrice)
                .filter(c -> maxPrice == null || c.getPricePerKwh().doubleValue() <= maxPrice)
                .filter(c -> {
                    if (userLat == null || userLng == null || radiusKm == null) return true;
                    return distanceInKm(userLat, userLng, c.getLatitude(), c.getLongitude()) <= radiusKm;
                })
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Map Charger entity to Response DTO
     */
    private ChargerResponseDto mapToResponse(Charger charger) {
        Long hostId = charger.getHost() != null ? charger.getHost().getUserId() : null;
        String hostEmail = charger.getHost() != null ? charger.getHost().getEmail() : null;

        return ChargerResponseDto.builder()
                .id(charger.getId())
                .name(charger.getName())
                .brand(charger.getBrand())
                .location(charger.getLocation())
                .latitude(charger.getLatitude())
                .longitude(charger.getLongitude())
                .pricePerKwh(charger.getPricePerKwh())
                .images(charger.getImages())
                .hostId(hostId)
                .hostEmail(hostEmail)
                .rating(charger.getRating())
                .build();
    }

    /**
     * Save uploaded images and return their URLs
     */
   /**
     * Upload images to Cloudinary and return their permanent URLs
     */
    private List<String> saveUploadedImages(List<MultipartFile> images) throws IOException {
        if (images == null || images.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> imageUrls = new ArrayList<>();

        for (MultipartFile file : images) {
            if (!file.isEmpty()) {
                String url = cloudinaryService.uploadFile(file, "ev-charging/chargers");
                imageUrls.add(url);
            }
        }

        return imageUrls;
    }

    /**
     * Delete old charger images from Cloudinary
     */
    private void deleteOldImages(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return;
        }

        for (String url : imageUrls) {
            cloudinaryService.deleteFile(url);
        }
    }

    /**
     * Calculate distance between two coordinates using Haversine formula
     */
    private double distanceInKm(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS_KM = 6371;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }
}