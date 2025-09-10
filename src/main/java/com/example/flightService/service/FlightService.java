package com.example.flightService.service;

import com.example.flightService.dto.FlightDto;
import com.example.flightService.dto.FlightSearchRequest;
import com.example.flightService.dto.SeatLockingDto;
import com.example.flightService.model.FlightEntity;
import com.example.flightService.model.ScheduleEntity;

import java.util.List;
import java.util.Optional;

public interface FlightService {

    List<FlightEntity> searchFlights(FlightSearchRequest request);

    Optional<FlightEntity> getFlightById(String id);

    FlightDto getFlightDetailsByScheduleId(String scheduleId);

    Optional<ScheduleEntity> getFlightDetailsByScheduleId2(String scheduleId);

    FlightEntity createFlight(FlightEntity flightEntity);

    SeatLockingDto lockSeats(String scheduleId, String orderId, Integer seatsToLock);

    boolean updatePaymentStatus(String orderId, String paymentStatus);
}
