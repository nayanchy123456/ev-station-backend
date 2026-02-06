package com.evstation.ev_charging_backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Rating Entity
 * Represents a user's rating and review for a charger after completing a booking.
 * Each booking can have only one rating to prevent spam and ensure authenticity.
 */
@Entity
@Table(
    name = "ratings",
    indexes = {
        @Index(name = "idx_rating_charger", columnList = "charger_id"),
        @Index(name = "idx_rating_user", columnList = "user_id"),
        @Index(name = "idx_rating_created", columnList = "created_at")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_rating_booking", columnNames = "booking_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The user who submitted this rating
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * The charger being rated
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "charger_id", nullable = false)
    private Charger charger;

    /**
     * The completed booking this rating is associated with
     * UNIQUE constraint ensures one rating per booking
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    /**
     * Rating score from 1 to 5 stars
     */
    @Column(name = "rating_score", nullable = false)
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must not exceed 5")
    private Integer ratingScore;

    /**
     * Optional review comment
     * Max 1000 characters to allow detailed feedback
     */
    @Column(name = "comment", columnDefinition = "TEXT")
    @Size(max = 1000, message = "Comment must not exceed 1000 characters")
    private String comment;

    /**
     * Timestamp when the rating was created
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the rating was last updated
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}