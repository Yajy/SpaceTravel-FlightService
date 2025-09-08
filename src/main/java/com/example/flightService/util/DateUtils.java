package com.example.flightService.util;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class DateUtils {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public LocalDate parseDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateString.trim(), DATE_FORMATTER);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date format. Expected format: yyyy-MM-dd, got: " + dateString);
        }
    }

    public int convertDateToDayOfWeek(LocalDate date) {
        // Convert LocalDate to day_of_week (0=Sunday, 1=Monday, etc.)
        return date.getDayOfWeek().getValue() % 7;
    }

    public String getDayName(int dayOfWeek) {
        // Convert day_of_week number to day name
        return switch (dayOfWeek) {
            case 0 -> "Sunday";
            case 1 -> "Monday";
            case 2 -> "Tuesday";
            case 3 -> "Wednesday";
            case 4 -> "Thursday";
            case 5 -> "Friday";
            case 6 -> "Saturday";
            default -> "Unknown";
        };
    }

    public List<LocalDate> getNextDatesForDayOfWeek(int dayOfWeek, int numberOfWeeks) {
        // Get next N dates that fall on the specified day of week
        List<LocalDate> dates = new ArrayList<>();
        LocalDate today = LocalDate.now();

        // Find the next occurrence of the specified day
        LocalDate nextDate = today;
        while (convertDateToDayOfWeek(nextDate) != dayOfWeek) {
            nextDate = nextDate.plusDays(1);
        }

        // Add the next N weeks of this day
        for (int i = 0; i < numberOfWeeks; i++) {
            dates.add(nextDate.plusWeeks(i));
        }

        return dates;
    }

    public int parseDateStringToDayOfWeek(String dateString) {
        // Parse YYYY-MM-DD format to day of week
        if (dateString == null || dateString.trim().isEmpty()) {
            return -1; // Invalid
        }

        try {
            LocalDate date = LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE);
            return convertDateToDayOfWeek(date);
        } catch (Exception e) {
            return -1; // Invalid date format
        }
    }
}
