package com.example.flightService.repository;

import com.example.flightService.model.ScheduleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduleRepository extends JpaRepository<ScheduleEntity, String> {

    List<ScheduleEntity> findByFlightFlightId(String flightId);

    List<ScheduleEntity> findByDayOfWeek(Integer dayOfWeek);

    @Query("SELECT s FROM ScheduleEntity s WHERE s.flight.flightId = :flightId AND s.dayOfWeek = :dayOfWeek")
    List<ScheduleEntity> findByFlightAndDay(@Param("flightId") String flightId,
                                           @Param("dayOfWeek") Integer dayOfWeek);

    @Query("SELECT s FROM ScheduleEntity s WHERE s.flight.sourceName = :sourceName AND s.flight.destinationName = :destinationName AND s.dayOfWeek = :dayOfWeek AND s.availabilityStatus = :status")
    List<ScheduleEntity> findByRouteAndDayAndStatus(@Param("sourceName") String sourceName,
                                                   @Param("destinationName") String destinationName,
                                                   @Param("dayOfWeek") Integer dayOfWeek,
                                                   @Param("status") ScheduleEntity.AvailabilityStatus status);

    @Query("SELECT s FROM ScheduleEntity s WHERE s.availableSeats > 0 AND s.dayOfWeek = :dayOfWeek")
    List<ScheduleEntity> findAvailableSchedulesByDay(@Param("dayOfWeek") Integer dayOfWeek);

    @Query("SELECT s FROM ScheduleEntity s JOIN FETCH s.flight WHERE s.scheduleId = :scheduleId")
    Optional<ScheduleEntity> findByIdWithFlight(@Param("scheduleId") String scheduleId);

    @Query("SELECT s FROM ScheduleEntity s WHERE s.flight.flightId = :flightId AND s.dayOfWeek = :dayOfWeek AND s.departureTime = :departureTime")
    Optional<ScheduleEntity> findByFlightDayAndTime(@Param("flightId") String flightId,
                                                   @Param("dayOfWeek") Integer dayOfWeek,
                                                   @Param("departureTime") LocalTime departureTime);
}
