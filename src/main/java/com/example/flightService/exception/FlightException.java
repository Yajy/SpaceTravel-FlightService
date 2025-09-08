package com.example.flightService.exception;

public class FlightException extends RuntimeException {

    public FlightException(String message) {
        super(message);
    }

    public FlightException(String message, Throwable cause) {
        super(message, cause);
    }
}
