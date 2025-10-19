package com.evstation.ev_charging_backend.repository;

import com.evstation.ev_charging_backend.entity.Charger;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChargerRepository extends JpaRepository<Charger, Long> {
    List<Charger> findByHostUserId(Long hostId);

}
