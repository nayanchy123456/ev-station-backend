package com.evstation.ev_charging_backend.dto;

import com.evstation.ev_charging_backend.enums.NotificationType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDto {

    private Long id;
    private NotificationType type;
    private String title;
    private String message;
    private Long bookingId;
    private Boolean read;
    private LocalDateTime createdAt;
}