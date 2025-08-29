package com.example.flightService.exception;

import io.grpc.Status;
import jakarta.persistence.EntityNotFoundException;
import net.devh.boot.grpc.server.advice.GrpcAdvice;
import net.devh.boot.grpc.server.advice.GrpcExceptionHandler;

@GrpcAdvice
public class GrpcExceptionAdvice {

    @GrpcExceptionHandler(FlightException.class)
    public Status handleFlightException(FlightException e) {
        return Status.INVALID_ARGUMENT.withDescription(e.getMessage());
    }

    @GrpcExceptionHandler(EntityNotFoundException.class)
    public Status handleNotFoundException(EntityNotFoundException e) {
        return Status.NOT_FOUND.withDescription(e.getMessage());
    }

    @GrpcExceptionHandler(IllegalArgumentException.class)
    public Status handleIllegalArgumentException(IllegalArgumentException e) {
        return Status.INVALID_ARGUMENT.withDescription(e.getMessage());
    }

    @GrpcExceptionHandler(Exception.class)
    public Status handleException(Exception e) {
        return Status.INTERNAL.withDescription("An internal server error occurred: " + e.getMessage());
    }
}
