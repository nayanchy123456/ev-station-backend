package com.evstation.ev_charging_backend.entity;

import com.evstation.ev_charging_backend.enums.UserPresenceStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Tracks the online/offline status of users in real-time.
 * 
 * Features:
 * - One presence record per user (unique constraint)
 * - Updated on WebSocket connect/disconnect events
 * - Stores last seen timestamp for offline users
 */
@Entity
@Table(
    name = "user_presence",
    indexes = {
        @Index(name = "idx_user_presence_user", columnList = "user_id"),
        @Index(name = "idx_user_presence_status", columnList = "status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPresence {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Reference to the user
     * One-to-one relationship with unique constraint
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;
    
    /**
     * Current presence status: ONLINE or OFFLINE
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private UserPresenceStatus status = UserPresenceStatus.OFFLINE;
    
    /**
     * Last time the user was seen online
     * Updated when:
     * - User connects (set to current time)
     * - User disconnects (set to disconnect time)
     * - Heartbeat received (optional, can update periodically)
     */
    @Column(name = "last_seen")
    private LocalDateTime lastSeen;
    
    /**
     * Auto-updated timestamp for tracking changes
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * Mark user as online
     */
    public void setOnline() {
        this.status = UserPresenceStatus.ONLINE;
        this.lastSeen = LocalDateTime.now();
    }
    
    /**
     * Mark user as offline
     */
    public void setOffline() {
        this.status = UserPresenceStatus.OFFLINE;
        this.lastSeen = LocalDateTime.now();
    }
    
    /**
     * Update last seen timestamp (for heartbeat)
     */
    public void updateLastSeen() {
        this.lastSeen = LocalDateTime.now();
    }
    
    /**
     * Check if user is currently online
     */
    public boolean isOnline() {
        return this.status == UserPresenceStatus.ONLINE;
    }
}