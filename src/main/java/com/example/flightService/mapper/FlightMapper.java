package com.example.flightService.mapper;

import com.example.flight.grpc.Flight;
import com.example.flightService.model.FlightEntity;
import com.google.protobuf.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

public class FlightMapper {
    private static Timestamp toTimestamp(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        Instant instant = dateTime.toInstant(ZoneOffset.UTC);
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }

    private static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        if (timestamp == null) return null;
        return LocalDateTime.ofInstant(
                Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos()),
                ZoneOffset.UTC);
    }

    public static Flight toProto(FlightEntity entity) {
        if (entity == null) return null;

        Flight.Builder builder = Flight.newBuilder()
                .setId(entity.getId() != null ? entity.getId().toString() : "")
                .setFlightName(entity.getFlightName())
                .setSourceName(entity.getSourceName())
                .setDestinationName(entity.getDestinationName())
                .setPrice(entity.getPrice())
                .setTotalSeats(entity.getTotalSeats())
                .setAvailableSeats(entity.getAvailableSeats())
                .setSeatType(entity.getSeatType())
                .setDepartureDate(entity.getDepartureDate())
                .setArrivalDate(entity.getArrivalDate())
                .setDepartureTime(entity.getDepartureTime())
                .setArrivalTime(entity.getArrivalTime())
                .setAvailability(entity.getAvailability())
                .setAdditionalInfo(entity.getAdditionalInfo())
                .setStatus(entity.getStatus());

        Timestamp createTime = toTimestamp(entity.getCreateTime());
        if (createTime != null) builder.setCreateTime(createTime);

        Timestamp updateTime = toTimestamp(entity.getUpdateTime());
        if (updateTime != null) builder.setUpdateTime(updateTime);

        return builder.build();
    }

    public static FlightEntity toEntity(Flight proto) {
        if (proto == null) return null;

        FlightEntity.FlightEntityBuilder builder = FlightEntity.builder()
                .id(proto.getId().isEmpty() ? null : UUID.fromString(proto.getId()))
                .flightName(proto.getFlightName())
                .sourceName(proto.getSourceName())
                .destinationName(proto.getDestinationName())
                .price(proto.getPrice())
                .totalSeats(proto.getTotalSeats())
                .availableSeats(proto.getAvailableSeats())
                .seatType(proto.getSeatType())
                .departureDate(proto.getDepartureDate())
                .arrivalDate(proto.getArrivalDate())
                .departureTime(proto.getDepartureTime())
                .arrivalTime(proto.getArrivalTime())
                .availability(proto.getAvailability())
                .additionalInfo(proto.getAdditionalInfo())
                .status(proto.getStatus());

        if (proto.hasCreateTime()) {
            builder.createTime(toLocalDateTime(proto.getCreateTime()));
        }
        if (proto.hasUpdateTime()) {
            builder.updateTime(toLocalDateTime(proto.getUpdateTime()));
        }

        return builder.build();
    }
}
