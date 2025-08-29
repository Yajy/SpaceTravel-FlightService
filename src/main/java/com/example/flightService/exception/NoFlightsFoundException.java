package com.example.flightService.exception;

public class NoFlightsFoundException extends RuntimeException {
    public NoFlightsFoundException(String message) {
        super(message);
    }
}
