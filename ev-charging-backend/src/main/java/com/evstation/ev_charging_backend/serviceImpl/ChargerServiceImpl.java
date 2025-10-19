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

}
