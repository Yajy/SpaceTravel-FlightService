package com.example.flightService;

import com.example.flightService.model.FlightEntity;
import com.example.flightService.repository.FlightRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.LocalDateTime;

@SpringBootApplication
public class FlightServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(FlightServiceApplication.class, args);
	}

	@Bean
	CommandLineRunner initDatabase(FlightRepository repository) {
		return args -> {
			// Add a sample flight for testing
			FlightEntity flight = FlightEntity.builder()
					.flightName("Test Flight 101")
					.sourceName("Mumbai")
					.destinationName("Delhi")
					.price(5000.0)
					.totalSeats(180)
					.availableSeats(100)
					.seatType("Economy")
					.departureDate("2025-08-29")  // Using tomorrow's date
					.arrivalDate("2025-08-29")
					.departureTime("10:00")
					.arrivalTime("12:00")
					.availability("Available")
					.additionalInfo("Direct Flight")
					.status(1)
					.createTime(LocalDateTime.now())
					.updateTime(LocalDateTime.now())
					.build();

			repository.save(flight);
			System.out.println("Sample flight added to database: " + flight.getId());
		};
	}
}
