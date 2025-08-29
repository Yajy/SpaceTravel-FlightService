package com.example.flightService.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "flights")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlightEntity {
    @Id
    @UuidGenerator(style = UuidGenerator.Style.AUTO)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private UUID id;

    @Column(name = "flight_name", nullable = false, length = 100)
    private String flightName;

    @Column(name = "source_name", nullable = false, length = 100)
    private String sourceName;

    @Column(name = "destination_name", nullable = false, length = 100)
    private String destinationName;

    @Column(name = "price", nullable = false)
    private Double price;

    @Column(name = "total_seats", nullable = false)
    private Integer totalSeats;

    @Column(name = "available_seats")
    private Integer availableSeats;

    @Column(name = "seat_type", length = 50)
    private String seatType;

    @Column(name = "departure_date")
    private String departureDate;

    @Column(name = "arrival_date")
    private String arrivalDate;

    @Column(name = "departure_time")
    private String departureTime;

    @Column(name = "arrival_time")
    private String arrivalTime;

    @Column(name = "availability")
    private String availability;

    @Column(name = "additional_info", length = 500)
    private String additionalInfo;

    @Column(name = "status")
    private Integer status;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
