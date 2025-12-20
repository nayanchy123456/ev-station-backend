package com.evstation.ev_charging_backend.service;

import com.evstation.ev_charging_backend.dto.BookingRequestDto;
import com.evstation.ev_charging_backend.dto.BookingResponseDto;

import java.util.List;

public interface BookingService {

    BookingResponseDto createBooking(BookingRequestDto dto, Long userId);

    List<BookingResponseDto> getMyBookings(Long userId);

    List<BookingResponseDto> getBookingsByCharger(Long chargerId);

    void cancelBooking(Long bookingId, Long userId);
}