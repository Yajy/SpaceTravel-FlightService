package com.example.flightService.mapper;

import com.example.flightService.model.FlightEntity;
import com.example.flightService.model.ScheduleEntity;
import com.example.flight.v1.grpc.Flight;
import com.example.flight.v1.grpc.Schedule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class FlightMapper {

    // Convert FlightEntity with schedules to gRPC Flight
    public Flight toGrpcFlight(FlightEntity flightEntity) {
        if (flightEntity == null) {
            return null;
        }

        Flight.Builder flightBuilder = Flight.newBuilder()
                .setFlightId(flightEntity.getFlightId() != null ? flightEntity.getFlightId() : "")
                .setFlightName(flightEntity.getFlightName() != null ? flightEntity.getFlightName() : "")
                .setSourceName(flightEntity.getSourceName() != null ? flightEntity.getSourceName() : "")
                .setDestinationName(flightEntity.getDestinationName() != null ? flightEntity.getDestinationName() : "")
                .setPrice(flightEntity.getPrice() != null ? flightEntity.getPrice().doubleValue() : 0.0)
                .setSeatType(flightEntity.getSeatType() != null ? flightEntity.getSeatType() : "")
                .setAdditionalInfo(flightEntity.getAdditionalInfo() != null ? flightEntity.getAdditionalInfo() : "");

        // Convert schedules
        List<Schedule> grpcSchedules = flightEntity.getSchedules().stream()
                .map(this::toGrpcSchedule)
                .collect(Collectors.toList());

        flightBuilder.addAllSchedules(grpcSchedules);

        return flightBuilder.build();
    }

    // Convert ScheduleEntity to gRPC Schedule
    public Schedule toGrpcSchedule(ScheduleEntity scheduleEntity) {
        if (scheduleEntity == null) {
            return null;
        }

        return Schedule.newBuilder()
                .setScheduleId(scheduleEntity.getScheduleId() != null ? scheduleEntity.getScheduleId() : "")
                .setDayOfWeek(scheduleEntity.getDayOfWeek() != null ? scheduleEntity.getDayOfWeek() : 0)
                .setDepartureTime(scheduleEntity.getDepartureTime() != null ? scheduleEntity.getDepartureTime().toString() : "")
                .setArrivalTime(scheduleEntity.getArrivalTime() != null ? scheduleEntity.getArrivalTime().toString() : "")
                .setAvailableSeats(scheduleEntity.getAvailableSeats() != null ? scheduleEntity.getAvailableSeats() : 0)
                .setAvailabilityStatus(scheduleEntity.getAvailabilityStatus() != null ?
                        scheduleEntity.getAvailabilityStatus().name() : "AVAILABLE")
                .build();
    }

    // Convert gRPC Flight to FlightEntity (for creating flights)
    public FlightEntity fromGrpcFlight(Flight grpcFlight) {
        if (grpcFlight == null) {
            return null;
        }

        FlightEntity flight = FlightEntity.builder()
                .flightName(grpcFlight.getFlightName())
                .sourceName(grpcFlight.getSourceName())
                .destinationName(grpcFlight.getDestinationName())
                .price(BigDecimal.valueOf(grpcFlight.getPrice()))
                .seatType(grpcFlight.getSeatType())
                .additionalInfo(grpcFlight.getAdditionalInfo())
                .build();

        // Convert schedules
        List<ScheduleEntity> schedules = grpcFlight.getSchedulesList().stream()
                .map(grpcSchedule -> fromGrpcSchedule(grpcSchedule, flight))
                .collect(Collectors.toList());

        flight.getSchedules().addAll(schedules);

        return flight;
    }

    // Convert gRPC Schedule to ScheduleEntity
    public ScheduleEntity fromGrpcSchedule(Schedule grpcSchedule, FlightEntity flight) {
        if (grpcSchedule == null) {
            return null;
        }

        return ScheduleEntity.builder()
                .dayOfWeek(grpcSchedule.getDayOfWeek())
                .departureTime(parseTime(grpcSchedule.getDepartureTime()))
                .arrivalTime(parseTime(grpcSchedule.getArrivalTime()))
                .availableSeats(grpcSchedule.getAvailableSeats())
                .availabilityStatus(parseAvailabilityStatus(grpcSchedule.getAvailabilityStatus()))
                .flight(flight)
                .build();
    }

    private LocalTime parseTime(String timeString) {
        try {
            return LocalTime.parse(timeString);
        } catch (Exception e) {
            return LocalTime.of(8, 0); // Default time
        }
    }

    private ScheduleEntity.AvailabilityStatus parseAvailabilityStatus(String statusString) {
        try {
            return ScheduleEntity.AvailabilityStatus.valueOf(statusString.toUpperCase());
        } catch (Exception e) {
            return ScheduleEntity.AvailabilityStatus.AVAILABLE;
        }
    }
}
