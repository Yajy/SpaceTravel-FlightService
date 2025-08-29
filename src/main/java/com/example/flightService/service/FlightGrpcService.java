package com.example.flightService.service;

import com.example.flight.grpc.*;
import com.example.flightService.exception.NoFlightsFoundException;
import com.example.flightService.mapper.FlightMapper;
import com.example.flightService.model.FlightEntity;
import com.google.protobuf.Empty;
import com.google.protobuf.Timestamp;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@GrpcService
public class FlightGrpcService extends FlightServiceGrpc.FlightServiceImplBase {
    private static final Logger logger = LoggerFactory.getLogger(FlightGrpcService.class);
    private final FlightService flightService;

    @Autowired
    public FlightGrpcService(FlightService flightService) {
        this.flightService = flightService;
    }

    @Override
    public void searchFlights(SearchFlightsRequest request, StreamObserver<SearchFlightsResponse> responseObserver) {
        try {
            String source = request.getSourceName();
            String destination = request.getDestinationName();
            String departureDate = request.getDepartureDate();

            logger.info("Searching flights with source: {}, destination: {}, departureDate: {}",
                       source, destination, departureDate);

            List<FlightEntity> flightEntities = flightService.searchFlight(source, destination, departureDate);

            List<Flight> protoFlights = flightEntities.stream()
                    .map(FlightMapper::toProto)
                    .toList();

            SearchFlightsResponse response = SearchFlightsResponse.newBuilder()
                    .addAllFlights(protoFlights)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (NoFlightsFoundException e) {
            logger.warn("No flights found: {}", e.getMessage());
            responseObserver.onError(
                Status.NOT_FOUND
                    .withDescription(e.getMessage())
                    .asRuntimeException()
            );
        } catch (Exception e) {
            logger.error("Error while searching flights", e);
            responseObserver.onError(
                Status.INTERNAL
                    .withDescription("Internal server error while searching flights")
                    .asRuntimeException()
            );
        }
    }

    @Override
    public void getFlightDetails(GetFlightDetailsRequest request, StreamObserver<GetFlightDetailsResponse> responseObserver) {
        try {
            UUID flightId = UUID.fromString(request.getFlightId());
            FlightEntity flightEntity = flightService.getFlightDetails(flightId)
                    .orElseThrow(() -> new EntityNotFoundException("Flight not found with id: " + flightId));

            GetFlightDetailsResponse response = GetFlightDetailsResponse.newBuilder()
                    .setFlight(FlightMapper.toProto(flightEntity))
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Invalid flight ID format")
                    .asRuntimeException());
        } catch (EntityNotFoundException e) {
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Error getting flight details: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void createFlight(CreateFlightRequest request, StreamObserver<CreateFlightResponse> responseObserver) {
        try {
            FlightEntity flightEntity = FlightMapper.toEntity(request.getFlight());
            FlightEntity createdFlight = flightService.createFlight(flightEntity);

            CreateFlightResponse response = CreateFlightResponse.newBuilder()
                    .setFlight(FlightMapper.toProto(createdFlight))
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Error creating flight: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void updateFlight(UpdateFlightRequest request, StreamObserver<UpdateFlightResponse> responseObserver) {
        try {
            UUID flightId = UUID.fromString(request.getFlightId());
            FlightEntity flightEntity = FlightMapper.toEntity(request.getFlight());
            flightEntity.setId(flightId);

            FlightEntity updatedFlight = flightService.updateFlight(flightId, flightEntity);

            UpdateFlightResponse response = UpdateFlightResponse.newBuilder()
                    .setFlight(FlightMapper.toProto(updatedFlight))
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Invalid flight ID format")
                    .asRuntimeException());
        } catch (EntityNotFoundException e) {
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Error updating flight: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void deleteFlight(DeleteFlightRequest request, StreamObserver<Empty> responseObserver) {
        try {
            UUID flightId = UUID.fromString(request.getFlightId());
            flightService.deleteFlight(flightId);

            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Invalid flight ID format")
                    .asRuntimeException());
        } catch (EntityNotFoundException e) {
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Error deleting flight: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    private LocalDate timestampToLocalDate(Timestamp timestamp) {
        return LocalDateTime.ofInstant(
            Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos()),
            ZoneOffset.UTC
        ).toLocalDate();
    }

    private Timestamp localDateTimeToTimestamp(LocalDateTime dateTime) {
        long epochSeconds = dateTime.toInstant(ZoneOffset.UTC).getEpochSecond();
        int nanos = dateTime.getNano();
        return Timestamp.newBuilder()
                .setSeconds(epochSeconds)
                .setNanos(nanos)
                .build();
    }
}
