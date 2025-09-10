package com.example.flightService.repository;

import com.example.flightService.model.FlightEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FlightRepository extends JpaRepository<FlightEntity, String> {

    @Query("SELECT f FROM FlightEntity f WHERE f.sourceName = :sourceName AND f.destinationName = :destinationName")
    List<FlightEntity> findBySourceAndDestination(@Param("sourceName") String sourceName,
                                                  @Param("destinationName") String destinationName);

    Optional<FlightEntity> findByFlightName(String flightName);

    @Query("SELECT f FROM FlightEntity f JOIN FETCH f.schedules s WHERE f.sourceName = :sourceName AND f.destinationName = :destinationName AND s.dayOfWeek = :dayOfWeek AND s.availableSeats > 0")
    List<FlightEntity> findBySourceDestinationAndDay(@Param("sourceName") String sourceName,
                                                     @Param("destinationName") String destinationName,
                                                     @Param("dayOfWeek") Integer dayOfWeek);

//    @Query("SELECT f FROM FlightEntity f JOIN FETCH f.schedules WHERE f.status = :status")
//    List<FlightEntity> findByStatusWithSchedules(@Param("status") FlightEntity.FlightStatus status);

    @Query("SELECT f FROM FlightEntity f LEFT JOIN FETCH f.schedules WHERE f.flightId = :flightId")
    Optional<FlightEntity> findByIdWithSchedules(@Param("flightId") String flightId);
}
