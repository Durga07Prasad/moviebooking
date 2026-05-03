package com.project.moviebooking.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Ticket Model - final confirmed ticket issued after payment
 * GRASP: Creator - Booking creates Ticket after payment success
 * Design Pattern: Observer - TicketObserver notifies user on ticket creation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "tickets")
public class Ticket {

    @Id
    private String id;

    private String bookingId;

    private String userId;

    private String userName;

    private String movieTitle;

    private String theatreName;

    private String showDate;

    private String showTime;

    private List<String> seatNumbers;

    private double totalAmount;

    private String ticketCode;    // unique QR/barcode identifier

    // VALID, USED, CANCELLED
    private String status = "VALID";

    private LocalDateTime issuedAt = LocalDateTime.now();
}
