package com.example.flightService.service;

import com.example.flightService.dto.FlightSearchRequest;
import com.example.flightService.exception.DuplicateFlightException;
import com.example.flightService.model.FlightEntity;
import com.example.flightService.model.ScheduleEntity;
import com.example.flightService.repository.FlightRepository;
import com.example.flightService.repository.ScheduleRepository;
import com.example.flightService.util.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class FlightServiceImpl implements FlightService {

    private static final Logger log = LoggerFactory.getLogger(FlightServiceImpl.class);
    private final DateUtils dateUtils;
    
    @Autowired
    private FlightRepository flightRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    public FlightServiceImpl(DateUtils dateUtils) {
        this.dateUtils = dateUtils;
    }

    @Override
    public List<FlightEntity> searchFlights(FlightSearchRequest request) {
        log.info("Searching flights with request: source={}, destination={}, dayOfWeek={}",
                request.getSourceName(), request.getDestinationName(), request.getDepartureDate());

        LocalDate searchDate = null;
        Integer dayOfWeek = null;

        if(request.getDepartureDate() != null && !request.getDepartureDate().trim().isEmpty()) {
            searchDate = dateUtils.parseDate(request.getDepartureDate());
            dayOfWeek = dateUtils.convertDateToDayOfWeek(searchDate);
            log.info("Converted dayOfWeek: {}", dayOfWeek);
        }

        List<FlightEntity> flights;
        
        if (dayOfWeek != null) {
            flights = flightRepository.findBySourceDestinationAndDay(
                request.getSourceName(), request.getDestinationName(), dayOfWeek);
        } else {
            flights = flightRepository.findBySourceAndDestination(
                request.getSourceName(), request.getDestinationName());
        }
        log.info("Found {} flights matching search criteria", flights.size());
        return flights;
    }

    @Override
    public Optional<FlightEntity> getFlightById(String flightId) {
        log.info("Getting flight by ID: {}", flightId);
        return flightRepository.findById(flightId);
    }

    @Override
    public Optional<FlightEntity> getFlightByIdWithSchedules(String flightId) {
        log.info("Getting flight by ID with schedules: {}", flightId);
        return flightRepository.findByIdWithSchedules(flightId);
    }

    @Override
    public Optional<FlightEntity> getFlightByName(String flightName) {
        log.info("Getting flight by name: {}", flightName);
        return flightRepository.findByFlightName(flightName);
    }

    @Override
    public FlightEntity createFlight(FlightEntity flight) {
        log.info("Creating flight: {}", flight.getFlightName());
        
        validateFlightData(flight);

        if (flightRepository.findByFlightName(flight.getFlightName()).isPresent()) {
            log.warn("Duplicate flight name detected: {}", flight.getFlightName());
            throw new DuplicateFlightException("Flight with this name already exists: " + flight.getFlightName());
        }

        FlightEntity savedFlight = flightRepository.save(flight);
        log.info("Flight created successfully with ID: {}", savedFlight.getFlightId());
        return savedFlight;
    }

    private void validateFlightData(FlightEntity flight) {
        log.info("Validating flight data for: {}", flight.getFlightName());

        if (flight.getFlightName() == null || flight.getFlightName().trim().isEmpty()) {
            throw new IllegalArgumentException("Flight name cannot be null or empty");
        }

        if (flight.getSourceName() == null || flight.getSourceName().trim().isEmpty()) {
            throw new IllegalArgumentException("Source name cannot be null or empty");
        }

        if (flight.getDestinationName() == null || flight.getDestinationName().trim().isEmpty()) {
            throw new IllegalArgumentException("Destination name cannot be null or empty");
        }

        if (flight.getSourceName().trim().equalsIgnoreCase(flight.getDestinationName().trim())) {
            throw new IllegalArgumentException("Source and destination cannot be the same");
        }

        if (flight.getPrice() == null || flight.getPrice().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Flight price must be greater than 0");
        }

        if (flight.getSeatType() == null || flight.getSeatType().trim().isEmpty()) {
            throw new IllegalArgumentException("Seat type cannot be null or empty");
        }

        if (flight.getSchedules() != null && !flight.getSchedules().isEmpty()) {
            for (ScheduleEntity schedule : flight.getSchedules()) {
                validateScheduleData(schedule);
            }
        }

        log.info("Flight data validation passed for: {}", flight.getFlightName());
    }

    private void validateScheduleData(ScheduleEntity schedule) {
        if (schedule.getDayOfWeek() == null || schedule.getDayOfWeek() < 0 || schedule.getDayOfWeek() > 6) {
            throw new IllegalArgumentException("Day of week must be between 0 (Sunday) and 6 (Saturday)");
        }

        if (schedule.getDepartureTime() == null) {
            throw new IllegalArgumentException("Departure time cannot be null");
        }

        if (schedule.getArrivalTime() == null) {
            throw new IllegalArgumentException("Arrival time cannot be null");
        }

        if (schedule.getDepartureTime().isAfter(schedule.getArrivalTime())) {
            throw new IllegalArgumentException("Departure time must be before arrival time");
        }

        if (schedule.getAvailableSeats() == null || schedule.getAvailableSeats() < 0) {
            throw new IllegalArgumentException("Available seats cannot be negative");
        }
    }

    @Override
    public boolean lockSeats(String scheduleId, String orderId, Integer seatsToLock) {
        log.info("Locking {} seats for schedule {} and order {}", seatsToLock, scheduleId, orderId);

        try {
            Optional<ScheduleEntity> scheduleOpt = scheduleRepository.findById(scheduleId);
            if (scheduleOpt.isEmpty()) {
                log.error("Schedule not found: {}", scheduleId);
                return false;
            }

            ScheduleEntity schedule = scheduleOpt.get();
            if (schedule.getAvailableSeats() < seatsToLock) {
                log.error("Not enough seats available. Requested: {}, Available: {}", 
                         seatsToLock, schedule.getAvailableSeats());
                return false;
            }

            // Temporarily reduce available seats
            schedule.setAvailableSeats(schedule.getAvailableSeats() - seatsToLock);
            scheduleRepository.save(schedule);
            
            log.info("Successfully locked {} seats for schedule {}", seatsToLock, scheduleId);
            return true;
        } catch (Exception e) {
            log.error("Failed to lock seats: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean updatePaymentStatus(String orderId, String paymentStatus) {
        log.info("Processing payment status update for order: {}, status: {}", orderId, paymentStatus);

        boolean paymentSuccess = "SUCCESS".equalsIgnoreCase(paymentStatus);
        
        // For now, we'll just log the payment status
        // In a real implementation, you'd need to track which seats were locked for this order
        log.info("Payment status updated for order: {}, success: {}", orderId, paymentSuccess);
        
        return true;
    }

    @Override
    public Optional<FlightEntity> getFlightDetailsByScheduleId(String scheduleId) {
        log.info("Getting flight details by schedule ID: {}", scheduleId);

        Optional<ScheduleEntity> scheduleOpt = scheduleRepository.findByIdWithFlight(scheduleId);
        
        if (scheduleOpt.isEmpty()) {
            log.warn("Schedule not found with ID: {}", scheduleId);
            return Optional.empty();
        }

        ScheduleEntity schedule = scheduleOpt.get();
        FlightEntity flight = schedule.getFlight();

        FlightEntity filteredFlight = new FlightEntity();
        filteredFlight.setFlightId(flight.getFlightId());
        filteredFlight.setFlightName(flight.getFlightName());
        filteredFlight.setSourceName(flight.getSourceName());
        filteredFlight.setDestinationName(flight.getDestinationName());
        filteredFlight.setPrice(flight.getPrice());
        filteredFlight.setSeatType(flight.getSeatType());
        filteredFlight.setAdditionalInfo(flight.getAdditionalInfo());

        filteredFlight.addSchedule(schedule);

        log.info("Found flight details for schedule {}: {}", scheduleId, flight.getFlightName());
        return Optional.of(filteredFlight);
    }
}
