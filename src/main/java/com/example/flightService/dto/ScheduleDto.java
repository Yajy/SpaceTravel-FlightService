package com.example.flightService.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleDto {
    private String scheduleId;
    private String departureTime;
    private String arrivalTime;
    private Integer availableSeats;
    private String availabilityStatus;
}
