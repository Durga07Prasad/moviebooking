package com.project.moviebooking.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Booking Model — represents a ticket reservation
 * SOLID: S — Single Responsibility, only holds booking state
 * GRASP: Creator — BookingService creates Booking (has all needed data)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "bookings")
public class Booking {

    @Id
    private String id;

    private String userId;

    private String showId;

    private String movieId;

    private String theatreId;

    private List<String> seatNumbers;

    private int numberOfSeats;

    private double totalAmount;

    /** CONFIRMED, CANCELLED, PENDING */
    private String bookingStatus = "PENDING";

    private String paymentId;

    private LocalDateTime bookingTime = LocalDateTime.now();
}
