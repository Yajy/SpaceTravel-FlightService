package com.example.flightService.service;

import com.example.flightService.dto.FlightDto;
import com.example.flightService.dto.SeatLockingDto;
import com.example.flightService.dto.UpdatePaymentStatusDto;
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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static java.awt.SystemColor.info;

@GrpcService
public class FlightGrpcService extends FlightServiceGrpc.FlightServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(FlightGrpcService.class);

    private final Lock seatLock = new ReentrantLock();

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

            List<FlightEntity> flightEntities = flightService.searchFlights(searchRequest);

            List<Flight> grpcFlights = flightEntities.stream()
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
        log.info("gRPC GetFlightDetails called for schedule ID: {}",
                request.getScheduleId());

        try {
            String scheduleId = request.getScheduleId();
            FlightDto flightDto = flightService.getFlightDetailsByScheduleId(scheduleId);

            if (flightDto != null) {
                int totalAvailableSeats = flightDto.getScheduleList().isEmpty() ? 0
                    : flightDto.getScheduleList().getFirst().getAvailableSeats();

                if (totalAvailableSeats <= 0) {
                    GetFlightDetailsResponse response = GetFlightDetailsResponse.newBuilder()
                            .setMessage("seat full")
                            .build();
                    responseObserver.onNext(response);
                } else {
                    Flight grpcFlight = flightMapper.toGrpcFlightFromDto(flightDto);
                    GetFlightDetailsResponse response = GetFlightDetailsResponse.newBuilder()
                            .setFlight(grpcFlight)
                            .build();
                    responseObserver.onNext(response);
                }
            } else {
                String message = "Flight not found or no schedule available for the requested schedule ID";

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
            seatLock.lock();
            SeatLockingDto lockResult = flightService.lockSeats(
                    request.getScheduleId(),
                    request.getOrderId(),
                    request.getSeatsToLock()
            );
            seatLock.unlock();

            log.info("Seat lock response from dto is : {}", lockResult.isSeatLocked());

            LockSeatsResponse response = LockSeatsResponse.newBuilder()
                    .setSuccess(lockResult.isSeatLocked())
                    .setMessage(lockResult.getMessage())
                    .build();


            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("LockSeats completed: {}", lockResult.isSeatLocked() ? "Success" : "Failed");

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
            UpdatePaymentStatusDto updateResult = flightService.updatePaymentStatus(
                    request.getOrderId(),
                    request.getPaymentStatus()
            );

            UpdatePaymentStatusResponse response = UpdatePaymentStatusResponse.newBuilder()
                    .setSuccess(updateResult.isSuccess())
                    .setMessage(updateResult.getMessage())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info(updateResult.getMessage());

        } catch (Exception e) {
            log.error("Error in UpdatePaymentStatus: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Error updating payment status: " + e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }
}
