package com.example.flightService.repository;

import com.example.flightService.model.FlightEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FlightRepository extends JpaRepository<FlightEntity, UUID> {
    List<FlightEntity> findBySourceNameIgnoreCaseAndDestinationNameIgnoreCase(String sourceName, String destinationName);

    List<FlightEntity> findBySourceName(String sourceName);
}
