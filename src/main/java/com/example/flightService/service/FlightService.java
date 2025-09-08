package com.example.flightService.service;

import com.example.flightService.dto.FlightSearchRequest;
import com.example.flightService.model.FlightEntity;

import java.util.List;
import java.util.Optional;

public interface FlightService {

    List<FlightEntity> searchFlights(FlightSearchRequest request);

    Optional<FlightEntity> getFlightById(String id);

    Optional<FlightEntity> getFlightByIdWithSchedules(String id);

    Optional<FlightEntity> getFlightByName(String flightName);

    Optional<FlightEntity> getFlightDetailsByScheduleId(String scheduleId);

    FlightEntity createFlight(FlightEntity flightEntity);

    boolean lockSeats(String scheduleId, String orderId, Integer seatsToLock);

    boolean updatePaymentStatus(String orderId, String paymentStatus);
}
