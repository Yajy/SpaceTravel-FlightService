package com.example.flightService.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FlightDto {
    private String flightId;
    private String flightName;
    private String sourceName;
    private String destinationName;
    private BigDecimal price;
    private String seatType;
    private String additionalInfo;
    private List<ScheduleDto> scheduleList;
}
