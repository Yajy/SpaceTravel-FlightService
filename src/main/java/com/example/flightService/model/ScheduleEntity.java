package com.example.flightService.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "schedules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleEntity {

    @Id
    @Column(name = "schedule_id", length = 36)
    private String scheduleId;

    @Column(name = "day_of_week", nullable = false)
    private Integer dayOfWeek; // 0=Sunday, 1=Monday, ..., 6=Saturday

    @Column(name = "departure_time", nullable = false)
    private LocalTime departureTime;

    @Column(name = "arrival_time", nullable = false)
    private LocalTime arrivalTime;

    @Column(name = "available_seats", nullable = false)
    private Integer availableSeats;

    @Enumerated(EnumType.STRING)
    @Column(name = "availability_status", nullable = false)
    private AvailabilityStatus availabilityStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id", nullable = false)
    private FlightEntity flight;

    @PrePersist
    public void generateId() {
        if (this.scheduleId == null) {
            this.scheduleId = UUID.randomUUID().toString();
        }
        if (this.availabilityStatus == null) {
            this.availabilityStatus = AvailabilityStatus.AVAILABLE;
        }
    }

    public enum AvailabilityStatus {
        AVAILABLE, FULL
    }

    // Helper method to check if schedule is available
    public boolean hasAvailableSeats() {
        return availableSeats != null && availableSeats > 0 && availabilityStatus == AvailabilityStatus.AVAILABLE;
    }

    // Helper method to update availability status based on available seats
    public void updateAvailabilityStatus() {
        this.availabilityStatus = hasAvailableSeats() ? AvailabilityStatus.AVAILABLE : AvailabilityStatus.FULL;
    }
}
