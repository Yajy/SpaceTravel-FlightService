package com.example.flightService.service;

import com.example.flightService.dto.FlightSearchRequest;
import com.example.flightService.model.FlightEntity;
import com.example.flightService.repository.FlightRepository;
import com.example.flightService.repository.ScheduleRepository;
import com.example.flightService.repository.TempOrderRepository;
import com.example.flightService.util.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.List;

import static com.google.common.base.Verify.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

public class FlightServiceImplTest {

    @Mock
    private FlightRepository flightRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private TempOrderRepository tempOrderRepository;

    @Mock
    private DateUtils dateUtils;

    @InjectMocks
    private FlightServiceImpl flightService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testSearchFlights() {
        FlightSearchRequest request = new FlightSearchRequest();
        request.setSourceName("DEL");
        request.setDestinationName("BOM");
        request.setDepartureDate("2025-09-15");

        LocalDate searchDate = LocalDate.of(2025, 9, 15);
        when(dateUtils.parseDate("2025-09-15")).thenReturn(searchDate);
        when(dateUtils.convertDateToDayOfWeek(searchDate)).thenReturn(1); // Monday

        FlightEntity flight = new FlightEntity();
        flight.setFlightId("F001");
        flight.setFlightName("Indigo");

        when(flightRepository.findBySourceDestinationAndDay("DEL", "BOM", 1))
                .thenReturn(List.of(flight));


        List<FlightEntity> result = flightService.searchFlights(request);


        assertEquals(1, result.size());
        assertEquals("Indigo", result.get(0).getFlightName());
    }
}
