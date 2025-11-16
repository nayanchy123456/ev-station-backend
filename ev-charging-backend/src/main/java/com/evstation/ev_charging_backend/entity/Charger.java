package com.evstation.ev_charging_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "chargers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Charger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String brand;

    @Column(nullable = false)
    private String location; // user entered location string

    private Double latitude;
    private Double longitude;

    @Column(nullable = false)
    private BigDecimal pricePerKwh;

    @ElementCollection
    @CollectionTable(name = "charger_images", joinColumns = @JoinColumn(name = "charger_id"))
    @Column(name = "image_url")
    @Builder.Default
    private List<String> images = List.of(); // default empty list

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id")
    private User host;

    @Builder.Default
    private Double rating = 0.0; // default rating
}