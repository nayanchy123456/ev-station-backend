package com.evstation.ev_charging_backend.specification;

import com.evstation.ev_charging_backend.entity.Charger;

import jakarta.persistence.criteria.Path;

import org.springframework.data.jpa.domain.Specification;
import java.math.BigDecimal;

public class ChargerSpecification {

    public static Specification<Charger> hasBrand(String brand) {
        return (root, query, cb) -> brand == null ? null :
                cb.like(cb.lower(root.get("brand")), "%" + brand.toLowerCase() + "%");
    }

    public static Specification<Charger> hasLocation(String location) {
        return (root, query, cb) -> location == null ? null :
                cb.like(cb.lower(root.get("location")), "%" + location.toLowerCase() + "%");
    }

    public static Specification<Charger> priceBetween(BigDecimal minPrice, BigDecimal maxPrice) {
        return (root, query, cb) -> {
            if (minPrice == null && maxPrice == null) return null;
            Path<BigDecimal> pricePath = root.get("pricePerKwh");
            if (minPrice != null && maxPrice != null) {
                return cb.between(pricePath, minPrice, maxPrice);
            } else if (minPrice != null) {
                return cb.greaterThanOrEqualTo(pricePath, minPrice);
            } else {
                return cb.lessThanOrEqualTo(pricePath, maxPrice);
            }
        };
    }

    public static Specification<Charger> ratingAtLeast(Double rating) {
        return (root, query, cb) -> rating == null ? null :
                cb.greaterThanOrEqualTo(root.get("rating"), rating);
    }

    // Note: For distance-based search we'd need Haversine in SQL or a spatial extension. Not included here.
}
