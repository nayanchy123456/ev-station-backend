package com.evstation.ev_charging_backend.serviceImpl;

import com.evstation.ev_charging_backend.dto.ChargerRequestDto;
import com.evstation.ev_charging_backend.dto.ChargerResponseDto;
import com.evstation.ev_charging_backend.entity.Charger;
import com.evstation.ev_charging_backend.entity.User;
import com.evstation.ev_charging_backend.exception.ResourceNotFoundException;
import com.evstation.ev_charging_backend.repository.ChargerRepository;
import com.evstation.ev_charging_backend.repository.UserRepository;
import com.evstation.ev_charging_backend.service.ChargerService;
import com.evstation.ev_charging_backend.util.GeoCodingUtil;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChargerServiceImpl implements ChargerService {

    private final ChargerRepository chargerRepository;
    private final UserRepository userRepository;
    private final GeoCodingUtil geoCodingUtil;

    public ChargerServiceImpl(ChargerRepository chargerRepository, UserRepository userRepository, GeoCodingUtil geoCodingUtil) {
        this.chargerRepository = chargerRepository;
        this.userRepository = userRepository;
        this.geoCodingUtil = geoCodingUtil;
    }

    @Override
    public ChargerResponseDto createCharger(ChargerRequestDto dto, Long hostId) {
        User host = userRepository.findById(hostId)
                .orElseThrow(() -> new ResourceNotFoundException("Host not found"));

        double[] latLng = geoCodingUtil.getLatLngFromAddress(dto.getLocation());

        Charger charger = Charger.builder()
                .name(dto.getName())
                .brand(dto.getBrand())
                .location(dto.getLocation())
                .latitude(latLng[0])
                .longitude(latLng[1])
                .pricePerKwh(dto.getPricePerKwh())
                .images(dto.getImages())
                .host(host)
                .rating(0.0)
                .build();

        chargerRepository.save(charger);

        return mapToResponse(charger);
    }

    @Override
    public ChargerResponseDto updateCharger(Long id, ChargerRequestDto dto, Long userId, String role) {
        Charger charger = chargerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Charger not found"));

        // Only HOST can update their own chargers; ADMIN can update all
        if ("HOST".equals(role) && !charger.getHost().getUserId().equals(userId)) {
            throw new AccessDeniedException("You are not the owner of this charger");
        }

        double[] latLng = geoCodingUtil.getLatLngFromAddress(dto.getLocation());

        charger.setName(dto.getName());
        charger.setBrand(dto.getBrand());
        charger.setLocation(dto.getLocation());
        charger.setLatitude(latLng[0]);
        charger.setLongitude(latLng[1]);
        charger.setPricePerKwh(dto.getPricePerKwh());
        charger.setImages(dto.getImages());

        chargerRepository.save(charger);

        return mapToResponse(charger);
    }

    @Override
    public void deleteCharger(Long id, Long userId, String role) {
        Charger charger = chargerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Charger not found"));

        // Only HOST can delete their own chargers; ADMIN can delete all
        if ("HOST".equals(role) && !charger.getHost().getUserId().equals(userId)) {
            throw new AccessDeniedException("You are not the owner of this charger");
        }

        chargerRepository.delete(charger);
    }

    @Override
    public List<ChargerResponseDto> getAllChargers() {
        return chargerRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ChargerResponseDto> getChargersByHost(Long hostId) {
        return chargerRepository.findByHostUserId(hostId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

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

@Override
public List<ChargerResponseDto> searchChargersNearby(
        String brand,
        String location,
        Double minPrice,
        Double maxPrice,
        Double userLat,
        Double userLng,
        Double radiusKm
) {
    try {
        List<Charger> chargers = chargerRepository.findAll();

        List<ChargerResponseDto> filtered = chargers.stream()
                // Filter by brand
                .filter(c -> brand == null || c.getBrand().toLowerCase().contains(brand.toLowerCase()))
                // Filter by location string
                .filter(c -> location == null || c.getLocation().toLowerCase().contains(location.toLowerCase()))
                // Filter by minPrice
                .filter(c -> minPrice == null || c.getPricePerKwh().doubleValue() >= minPrice)
                // Filter by maxPrice
                .filter(c -> maxPrice == null || c.getPricePerKwh().doubleValue() <= maxPrice)
                // Filter by distance if coordinates provided
                .filter(c -> {
                    if (userLat == null || userLng == null || radiusKm == null) return true;
                    if (c.getLatitude() == null || c.getLongitude() == null) return false;
                    double distance = distanceInKm(userLat, userLng, c.getLatitude(), c.getLongitude());
                    return distance <= radiusKm;
                })
                // Sort by nearest if coordinates provided
                .sorted((c1, c2) -> {
                    if (userLat == null || userLng == null) return 0;
                    double d1 = distanceInKm(userLat, userLng, c1.getLatitude(), c1.getLongitude());
                    double d2 = distanceInKm(userLat, userLng, c2.getLatitude(), c2.getLongitude());
                    return Double.compare(d1, d2);
                })
                .map(this::mapToResponse)
                .toList();

        if (filtered.isEmpty()) {
            throw new ResourceNotFoundException("No chargers found matching the given criteria");
        }

        return filtered;

    } catch (ResourceNotFoundException e) {
        throw e; // Let GlobalExceptionHandler handle it
    } catch (Exception e) {
        throw new RuntimeException("Error while searching chargers: " + e.getMessage());
    }
}

@Override
public List<ChargerResponseDto> findNearbyChargers(double latitude, double longitude, double radiusKm) {
    try {
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Invalid latitude or longitude values");
        }

        List<Charger> chargers = chargerRepository.findAll();

        List<ChargerResponseDto> nearby = chargers.stream()
                .filter(c -> c.getLatitude() != null && c.getLongitude() != null)
                .filter(c -> distanceInKm(latitude, longitude, c.getLatitude(), c.getLongitude()) <= radiusKm)
                .map(this::mapToResponse)
                .toList();

        if (nearby.isEmpty()) {
            throw new ResourceNotFoundException("No chargers found within the given radius");
        }

        return nearby;

    } catch (ResourceNotFoundException | IllegalArgumentException e) {
        throw e; // Let GlobalExceptionHandler handle it
    } catch (Exception e) {
        throw new RuntimeException("Error while finding nearby chargers: " + e.getMessage());
    }
}

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


@Override
public List<ChargerResponseDto> searchChargers(String brand, String location, Double minPrice, Double maxPrice) {
    try {
        List<Charger> chargers = chargerRepository.findAll();

        List<ChargerResponseDto> filtered = chargers.stream()
                .filter(c -> brand == null || c.getBrand().toLowerCase().contains(brand.toLowerCase()))
                .filter(c -> location == null || c.getLocation().toLowerCase().contains(location.toLowerCase()))
                .filter(c -> minPrice == null || c.getPricePerKwh().doubleValue() >= minPrice)
                .filter(c -> maxPrice == null || c.getPricePerKwh().doubleValue() <= maxPrice)
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            throw new ResourceNotFoundException("No chargers found for the given search criteria");
        }

        return filtered;
    } catch (Exception e) {
        throw new RuntimeException("Error while filtering chargers: " + e.getMessage());
    }
}







}
