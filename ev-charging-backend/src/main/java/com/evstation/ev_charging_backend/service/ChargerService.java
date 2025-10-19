package com.evstation.ev_charging_backend.service;

import com.evstation.ev_charging_backend.dto.ChargerRequestDto;
import com.evstation.ev_charging_backend.dto.ChargerResponseDto;

import java.util.List;

public interface ChargerService {
    ChargerResponseDto createCharger(ChargerRequestDto dto, Long hostId);
    ChargerResponseDto updateCharger(Long id, ChargerRequestDto dto, Long userId, String role);
    void deleteCharger(Long id, Long userId, String role);
    List<ChargerResponseDto> getAllChargers();
    List<ChargerResponseDto> getChargersByHost(Long hostId);
}
