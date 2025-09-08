package com.example.flightService.service;

import com.example.flightService.exception.DuplicateFlightException;
import com.example.flightService.model.FlightEntity;
import com.example.flightService.model.ScheduleEntity;
import com.example.flightService.dto.FlightSearchRequest;
import com.example.flightService.mapper.FlightMapper;
import com.example.flightService.repository.ScheduleRepository;
//import com.example.flightService.exception.DuplicateFlightException;
import com.example.flight.v1.grpc.*;
import com.example.flightService.util.DateUtils;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@GrpcService
public class FlightGrpcService extends FlightServiceGrpc.FlightServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(FlightGrpcService.class);

    @Autowired
    private FlightService flightService;

    @Autowired
    private FlightMapper flightMapper;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Override
    public void searchFlights(SearchFlightsRequest request, StreamObserver<SearchFlightsResponse> responseObserver) {
        log.info("gRPC SearchFlights called with source: {}, destination: {}, dayOfWeek: {}",
                request.getSourceName(), request.getDestinationName(), request.getDepartureDate());

        try {
            FlightSearchRequest searchRequest = new FlightSearchRequest(
                request.getSourceName(),
                request.getDestinationName(),
                request.getDepartureDate()
            );

            List<FlightEntity> flights = flightService.searchFlights(searchRequest);

            List<Flight> grpcFlights = flights.stream()
                    .map(flightMapper::toGrpcFlight)
                    .collect(Collectors.toList());

            SearchFlightsResponse response = SearchFlightsResponse.newBuilder()
                    .addAllFlights(grpcFlights)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("SearchFlights completed successfully, found {} flights", grpcFlights.size());

        } catch (Exception e) {
            log.error("Error in SearchFlights: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Internal server error while searching flights")
                    .withCause(e)
                    .asRuntimeException());
        }
    }

    @Override
    public void getFlightDetails(GetFlightDetailsRequest request, StreamObserver<GetFlightDetailsResponse> responseObserver) {
        log.info("gRPC GetFlightDetails called for flight ID: {} and day: {}",
                request.getScheduleId(), request.getDepartureDate());

        try {
            String scheduleId = request.getScheduleId();
            //Integer dayOfWeek = request.getDayOfWeek() != 0 ? request.getDayOfWeek() : null;

            Optional<FlightEntity> flightOpt = flightService.getFlightDetailsByScheduleId(scheduleId);

            if (flightOpt.isPresent()) {
                FlightEntity flight = flightOpt.get();

                int totalAvailableSeats = scheduleRepository.findById(scheduleId)
                        .map(ScheduleEntity::getAvailableSeats)
                        .orElse(0);

                if (totalAvailableSeats <= 0) {
                    GetFlightDetailsResponse response = GetFlightDetailsResponse.newBuilder()
                            .setMessage("seat full")
                            .build();
                    responseObserver.onNext(response);
                } else {
                    Flight grpcFlight = flightMapper.toGrpcFlight(flight);
                    GetFlightDetailsResponse response = GetFlightDetailsResponse.newBuilder()
                            .setFlight(grpcFlight)
                            .build();
                    responseObserver.onNext(response);
                }
            } else {
                String message = "Flight not found or no schedule available for the requested day" ;

                GetFlightDetailsResponse response = GetFlightDetailsResponse.newBuilder()
                        .setMessage(message)
                        .build();
                responseObserver.onNext(response);
            }
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error in GetFlightDetails: {}", e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Internal server error: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void createFlight(CreateFlightRequest request, StreamObserver<CreateFlightResponse> responseObserver) {
        log.info("gRPC CreateFlight called for flight: {}", request.getFlight().getFlightName());

        try {
            FlightEntity flightEntity = flightMapper.fromGrpcFlight(request.getFlight());
            FlightEntity savedFlight = flightService.createFlight(flightEntity);

            Flight grpcFlight = flightMapper.toGrpcFlight(savedFlight);
            CreateFlightResponse response = CreateFlightResponse.newBuilder()
                    .setFlight(grpcFlight)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("CreateFlight completed successfully for flight: {}", savedFlight.getFlightName());

        } catch (DuplicateFlightException e) {
            log.warn("Duplicate flight name detected: {}", e.getMessage());
            responseObserver.onError(Status.ALREADY_EXISTS
                    .withDescription("Flight already exists: " + e.getMessage())
                    .asRuntimeException());
        } catch (Exception e) {
            log.error("Error in CreateFlight: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Error creating flight: " + e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }

    @Override
    public void lockSeats(LockSeatsRequest request, StreamObserver<LockSeatsResponse> responseObserver) {
        log.info("gRPC LockSeats called for schedule: {}, order: {}, seats: {}",
                request.getScheduleId(), request.getOrderId(), request.getSeatsToLock());

        try {
            boolean lockResult = flightService.lockSeats(
                    request.getScheduleId(),  // Now using scheduleId instead of flightId
                    request.getOrderId(),
                    request.getSeatsToLock()
            );

            LockSeatsResponse response = LockSeatsResponse.newBuilder()
                    .setSuccess(lockResult)
                    .setMessage(lockResult ? "Seats locked successfully" : "Failed to lock seats")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("LockSeats completed: {}", lockResult ? "Success" : "Failed");

        } catch (Exception e) {
            log.error("Error in LockSeats: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Error locking seats: " + e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }

    @Override
    public void updatePaymentStatus(UpdatePaymentStatusRequest request, StreamObserver<UpdatePaymentStatusResponse> responseObserver) {
        log.info("gRPC UpdatePaymentStatus called for order: {}, status: {}",
                request.getOrderId(), request.getPaymentStatus());

        try {
            boolean updateResult = flightService.updatePaymentStatus(
                    request.getOrderId(),
                    request.getPaymentStatus()
            );

            UpdatePaymentStatusResponse response = UpdatePaymentStatusResponse.newBuilder()
                    .setSuccess(updateResult)
                    .setMessage(updateResult ? "Payment status updated successfully" : "Failed to update payment status")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("UpdatePaymentStatus completed: {}", updateResult ? "Success" : "Failed");

        } catch (Exception e) {
            log.error("Error in UpdatePaymentStatus: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Error updating payment status: " + e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }
}
