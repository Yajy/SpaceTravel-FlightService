package com.example.flightService.service;

import com.example.flightService.model.FlightEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FlightService {
    List<FlightEntity> searchFlight(String sourceName, String destinationName, String journeyStartDate);
    Optional<FlightEntity> getFlightDetails(UUID flightId);

    FlightEntity createFlight(FlightEntity flightEntity);
    FlightEntity updateFlight(UUID id, FlightEntity flightEntity);
    void deleteFlight(UUID flightId);
}
