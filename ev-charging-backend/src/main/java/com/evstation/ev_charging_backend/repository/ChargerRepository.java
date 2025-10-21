package com.evstation.ev_charging_backend.repository;

import com.evstation.ev_charging_backend.entity.Charger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChargerRepository extends JpaRepository<Charger, Long> {

    List<Charger> findByHostUserId(Long hostId);

    // 🔍 Search query with optional filters
    @Query("""
        SELECT c FROM Charger c
        WHERE (:brand IS NULL OR LOWER(c.brand) LIKE LOWER(CONCAT('%', :brand, '%')))
        AND (:location IS NULL OR LOWER(c.location) LIKE LOWER(CONCAT('%', :location, '%')))
        AND (:minPrice IS NULL OR c.pricePerKwh >= :minPrice)
        AND (:maxPrice IS NULL OR c.pricePerKwh <= :maxPrice)
        """)
    List<Charger> searchChargers(
            @Param("brand") String brand,
            @Param("location") String location,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice
    );
}
