package com.example.flightService.service;

import com.example.flightService.exception.NoFlightsFoundException;
import com.example.flightService.model.FlightEntity;
import com.example.flightService.repository.FlightRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import jakarta.persistence.EntityNotFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class FlightServiceImpl implements FlightService {
    private static final Logger logger = LoggerFactory.getLogger(FlightServiceImpl.class);
    private final FlightRepository flightRepository;

    public FlightServiceImpl(FlightRepository flightRepository) {
        this.flightRepository = flightRepository;
    }

    @Override
    public List<FlightEntity> searchFlight(String sourceName, String destinationName, String journeyStartDate) {
        logger.info("Searching flights - source: '{}', destination: '{}', date: '{}'",
                sourceName, destinationName, journeyStartDate);

        List<FlightEntity> flights = flightRepository.findBySourceNameIgnoreCaseAndDestinationNameIgnoreCase(sourceName, destinationName);

        if (flights.isEmpty()) {
            String message = String.format("No flights found from %s to %s", sourceName, destinationName);
            logger.warn(message);
            throw new NoFlightsFoundException(message);
        }

        logger.info("Found {} flights matching criteria", flights.size());
        return flights;
    }

    @Override
    public Optional<FlightEntity> getFlightDetails(UUID id) {
        return flightRepository.findById(id);
    }

    @Override
    public FlightEntity createFlight(FlightEntity flightEntity) {
        flightEntity.setCreateTime(LocalDateTime.now());
        flightEntity.setUpdateTime(LocalDateTime.now());
        return flightRepository.save(flightEntity);
    }

    @Override
    public FlightEntity updateFlight(UUID id, FlightEntity flightEntity) {
        if (!flightRepository.existsById(id)) {
            throw new EntityNotFoundException("Flight not found with id: " + id);
        }
        flightEntity.setId(id);
        flightEntity.setUpdateTime(LocalDateTime.now());
        return flightRepository.save(flightEntity);
    }

    @Override
    public void deleteFlight(UUID id) {
        if (!flightRepository.existsById(id)) {
            throw new EntityNotFoundException("Flight not found with id: " + id);
        }
        flightRepository.deleteById(id);
    }
}
