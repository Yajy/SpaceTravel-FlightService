package com.example.flightService.service;

import com.example.flightService.dto.FlightDto;
import com.example.flightService.dto.FlightSearchRequest;
import com.example.flightService.dto.ScheduleDto;
import com.example.flightService.dto.SeatLockingDto;
import com.example.flightService.exception.DuplicateFlightException;
import com.example.flightService.model.FlightEntity;
import com.example.flightService.model.ScheduleEntity;
import com.example.flightService.model.TempOrders;
import com.example.flightService.repository.FlightRepository;
import com.example.flightService.repository.ScheduleRepository;
import com.example.flightService.repository.TempOrderRepository;
import com.example.flightService.util.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @Autowired
    private TempOrderRepository tempOrderRepository;

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

    public Optional<FlightEntity> getFlightByIdWithSchedules(String flightId) {
        log.info("Getting flight by ID with schedules: {}", flightId);
        return flightRepository.findByIdWithSchedules(flightId);
    }

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
    public SeatLockingDto lockSeats(String scheduleId, String orderId, Integer seatsToLock) {
        log.info("Locking {} seats for schedule {} and order {}", seatsToLock, scheduleId, orderId);

        String message = "";
        boolean seatLocked = false;

        try {
            Optional<TempOrders> existingOrder = tempOrderRepository.findByOrderIdAndStatus(orderId, TempOrders.OrderStatus.PENDING);
            if (existingOrder.isPresent()) {
                log.warn("Order {} already has pending seat lock", orderId);
                message= "Seats Already locked for this order!";
            }

            Optional<ScheduleEntity> scheduleOpt = scheduleRepository.findById(scheduleId);
            if (scheduleOpt.isEmpty()) {
                log.error("Schedule not found: {}", scheduleId);
                message= "Schedule not found "+ scheduleId;
            }

            ScheduleEntity schedule = scheduleOpt.get();
            if (schedule.getAvailableSeats() < seatsToLock) {
                log.error("Not enough seats available. Requested: {}, Available: {}", 
                         seatsToLock, schedule.getAvailableSeats());
                message= "Not enough seats available: "+ seatsToLock+" Available: " + schedule.getAvailableSeats();
            }

            TempOrders tempOrder = TempOrders.builder()
                    .orderId(orderId)
                    .scheduleId(scheduleId)
                    .seatsLocked(seatsToLock)
                    .status(TempOrders.OrderStatus.PENDING)
                    .build();

            tempOrderRepository.save(tempOrder);

            schedule.setAvailableSeats(schedule.getAvailableSeats() - seatsToLock);
            scheduleRepository.save(schedule);
        } catch (Exception e) {
            log.error("Failed to lock seats: {}", e.getMessage(), e);
            message = "Failed to lock seats: " + e.getMessage();
        }
        if(message.isEmpty()) {
            seatLocked = true;
            message = "Seats locked successfully";
            log.info("Seats locked successfully for order {}", orderId);
        } else {
            log.warn("Seat locking failed for order {}: {}", orderId, message);
        }
        return SeatLockingDto.builder()
                .seatLocked(seatLocked)
                .message(message)
                .build();
    }

    @Override
    public boolean updatePaymentStatus(String orderId, String paymentStatus) {
        log.info("Processing payment status update for order: {}, status: {}", orderId, paymentStatus);

        try {
//            List<TempOrders> allOrdersWithId = tempOrderRepository.findAll().stream()
//                    .filter(order -> order.getOrderId().equals(orderId))
//                    .toList();
//
//            log.info("Found {} temp orders with orderId {}", allOrdersWithId.size(), orderId);
//
//            if (!allOrdersWithId.isEmpty()) {
//                TempOrders existingOrder = allOrdersWithId.get(0);
//                log.info("Existing order status: {}, expiresAt: {}, current time: {}",
//                        existingOrder.getStatus(), existingOrder.getExpiresAt(), LocalDateTime.now());
//            }

            Optional<TempOrders> tempOrderOpt = tempOrderRepository.findByOrderIdAndStatus(orderId, TempOrders.OrderStatus.PENDING);
            if (tempOrderOpt.isEmpty()) {
                log.warn("No pending temp order found for order ID: {}. Check if order exists with different status or has expired.", orderId);
                return false;
            }

            TempOrders tempOrder = tempOrderOpt.get();
            boolean paymentSuccess = "SUCCESS".equalsIgnoreCase(paymentStatus);

            if (paymentSuccess) {
                tempOrder.setStatus(TempOrders.OrderStatus.COMPLETED);
                tempOrderRepository.save(tempOrder);
                log.info("Payment successful for order: {}. Seats permanently reserved.", orderId);
            } else {
                Optional<ScheduleEntity> scheduleOpt = scheduleRepository.findById(tempOrder.getScheduleId());
                if (scheduleOpt.isPresent()) {
                    ScheduleEntity schedule = scheduleOpt.get();
                    schedule.setAvailableSeats(schedule.getAvailableSeats() + tempOrder.getSeatsLocked());
                    scheduleRepository.save(schedule);
                    log.info("Restored {} seats to schedule {} due to payment failure",
                             tempOrder.getSeatsLocked(), tempOrder.getScheduleId());
                }

                tempOrder.setStatus(TempOrders.OrderStatus.FAILED);
                tempOrderRepository.save(tempOrder);
                log.info("Payment failed for order: {}. Seats restored.", orderId);
            }

            return true;
        } catch (Exception e) {
            log.error("Failed to update payment status for order {}: {}", orderId, e.getMessage(), e);
            return false;
        }
    }


    @Override
    public FlightDto getFlightDetailsByScheduleId(String scheduleId) {
        log.info("Getting flight details by schedule ID: {}", scheduleId);

        Optional<ScheduleEntity> scheduleOpt = scheduleRepository.findByIdWithFlight(scheduleId);

        if (scheduleOpt.isEmpty()) {
            log.warn("Schedule not found with ID: {}", scheduleId);
            return null;
        }

        ScheduleEntity schedule = scheduleOpt.get();
        FlightEntity flight = schedule.getFlight();

        ScheduleDto scheduleDto = ScheduleDto.builder()
                .scheduleId(schedule.getScheduleId())
                .departureTime(schedule.getDepartureTime().toString())
                .arrivalTime(schedule.getArrivalTime().toString())
                .availableSeats(schedule.getAvailableSeats())
                .availabilityStatus(schedule.getAvailabilityStatus().toString())
                .build();

        FlightDto flightResponseDto = FlightDto.builder()
                .flightId(flight.getFlightId())
                .flightName(flight.getFlightName())
                .sourceName(flight.getSourceName())
                .destinationName(flight.getDestinationName())
                .price(flight.getPrice())
                .seatType(flight.getSeatType())
                .additionalInfo(flight.getAdditionalInfo())
                .scheduleList(List.of(scheduleDto))
                .build();

        log.info("Found flight details for schedule {}: {}", scheduleId, flight.getFlightName());
        return flightResponseDto;
    }

    @Override
    public Optional<ScheduleEntity> getFlightDetailsByScheduleId2(String scheduleId) {
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
        return Optional.of(schedule);
    }
}
