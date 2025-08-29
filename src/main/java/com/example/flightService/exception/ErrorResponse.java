package com.example.flightService.exception;

import io.grpc.Status;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ErrorResponse {
    private Status.Code statusCode;
    private String message;
    private String details;

    public static ErrorResponse fromStatus(Status status) {
        return ErrorResponse.builder()
                .statusCode(status.getCode())
                .message(status.getDescription())
                .details(status.getCause() != null ? status.getCause().getMessage() : null)
                .build();
    }
}
