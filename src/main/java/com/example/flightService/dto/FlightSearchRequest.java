package com.example.flightService.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlightSearchRequest {
    private String sourceName;
    private String destinationName;
    private String departureDate; // Keep for backward compatibility
//    private String dayOfWeek; // New field: 0=Sunday, 1=Monday, ..., 6=Saturday

    // Constructor for backward compatibility
//    public FlightSearchRequest(String sourceName, String destinationName, String departureDate) {
//        this.sourceName = sourceName;
//        this.destinationName = destinationName;
//        this.departureDate = departureDate;
//    }

    // Helper method to convert date strings to day of week
//    private Integer convertDateStringToDayOfWeek(String dateString) {
//        if (dateString == null || dateString.isEmpty()) {
//            return null;
//        }
//        LocalDate searchDate = null;
//        Integer dayOfWeek = null;
//
//        // Simple mapping for common day names - you can enhance this
//        return switch (dateString.toUpperCase()) {
//            case "SUNDAY", "SUN" -> 0;
//            case "MONDAY", "MON" -> 1;
//            case "TUESDAY", "TUE" -> 2;
//            case "WEDNESDAY", "WED" -> 3;
//            case "THURSDAY", "THU" -> 4;
//            case "FRIDAY", "FRI" -> 5;
//            case "SATURDAY", "SAT" -> 6;
//            default -> null; // For actual dates, you'd parse and get day of week
//        };
//    }
}
