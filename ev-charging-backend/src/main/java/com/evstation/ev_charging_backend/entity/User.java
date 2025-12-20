package com.evstation.ev_charging_backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import com.evstation.ev_charging_backend.enums.Role;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    private String firstName;
    private String lastName;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(unique = true, nullable = false)
    @NotBlank(message = "Phone number is required")
    @Pattern(
        regexp = "^[8-9]\\d{9}$|^\\+?977[8-9]\\d{9}$",
        message = "Phone number must be a valid Nepal number with exactly 10 digits (starting with 8 or 9), optionally with country code (+977)"
    )
    private String phone;

    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
