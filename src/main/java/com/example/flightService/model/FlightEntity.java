package com.example.flightService.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "flights")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlightEntity {

    @Id
    @Column(name = "flight_id", length = 36)
    private String flightId;

    @Column(name = "flight_name", unique = true, nullable = false)
    private String flightName;

    @Column(name = "source_name", nullable = false)
    private String sourceName;

    @Column(name = "destination_name", nullable = false)
    private String destinationName;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "seat_type")
    private String seatType;

    @Column(name = "additional_info")
    private String additionalInfo;

    @OneToMany(mappedBy = "flight", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ScheduleEntity> schedules = new ArrayList<>();

    @PrePersist
    public void generateId() {
        if (this.flightId == null) {
            this.flightId = UUID.randomUUID().toString();
        }
    }

    // Helper methods for managing schedules
    public void addSchedule(ScheduleEntity schedule) {
        schedules.add(schedule);
        schedule.setFlight(this);
    }

    public void removeSchedule(ScheduleEntity schedule) {
        schedules.remove(schedule);
        schedule.setFlight(null);
    }
}
