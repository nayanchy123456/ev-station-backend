package com.evstation.ev_charging_backend.service;

import com.evstation.ev_charging_backend.dto.ReservationRequestDto;
import com.evstation.ev_charging_backend.dto.ReservationResponseDto;

import java.util.List;

public interface ReservationService {
    
    ReservationResponseDto createReservation(ReservationRequestDto dto, Long userId);
    
    ReservationResponseDto getReservationById(Long reservationId, Long userId);
    
    List<ReservationResponseDto> getMyReservations(Long userId);
    
    void validateReservation(Long reservationId, Long userId);
    
    void checkReservationExpiry(Long reservationId);
}