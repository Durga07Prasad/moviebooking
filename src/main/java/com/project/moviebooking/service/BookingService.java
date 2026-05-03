package com.project.moviebooking.service;

import com.project.moviebooking.dto.BookingRequest;
import com.project.moviebooking.model.Booking;
import com.project.moviebooking.model.Ticket;

import java.util.List;

/**
 * BookingService — Interface (Dependency Inversion Principle)
 * ============================================================
 * SOLID: D — Dependency Inversion Principle
 *
 * HIGH-LEVEL modules (BookingController) depend on THIS abstraction.
 * LOW-LEVEL module (BookingServiceImpl) implements this interface.
 *
 * BookingController does:
 *   @Autowired BookingService bookingService;   ← depends on interface
 *   NOT:
 *   @Autowired BookingServiceImpl bookingService; ← would violate DIP
 *
 * Spring injects BookingServiceImpl at runtime — controller never imports it.
 *
 * GRASP: Controller — BookingController is the GRASP controller,
 *        this interface is its dependency contract.
 * ============================================================
 */
public interface BookingService {

    /** Create booking — validate + lock seats → return PENDING booking */
    Booking createBooking(BookingRequest request, String userId);

    /** Confirm booking + issue ticket via TicketFactory (called after payment) */
    Ticket confirmBookingAndIssueTicket(String bookingId, String paymentId, String userEmail);

    /** Cancel booking → release seats → CANCELLED status */
    Booking cancelBooking(String bookingId, String userId);

    /** Get single booking by ID */
    Booking getBookingById(String id);

    /** Get all bookings for a user */
    List<Booking> getBookingsByUser(String userId);

    /** Get ticket by booking ID */
    Ticket getTicketByBookingId(String bookingId);

    /** Get ticket by ticket ID */
    Ticket getTicketById(String ticketId);

    /** Get all tickets for a user */
    List<Ticket> getTicketsByUser(String userId);

    /** Admin: get all bookings */
    List<Booking> getAllBookings();
}
