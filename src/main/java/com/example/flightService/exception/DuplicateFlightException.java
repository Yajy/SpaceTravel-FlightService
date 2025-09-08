package com.example.flightService.exception;

public class DuplicateFlightException extends FlightException {

    public DuplicateFlightException(String message) {
        super(message);
    }

    public DuplicateFlightException(String message, Throwable cause) {
        super(message, cause);
    }
}
