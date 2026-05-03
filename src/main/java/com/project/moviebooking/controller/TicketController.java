package com.project.moviebooking.controller;

import com.project.moviebooking.config.JwtUtil;
import com.project.moviebooking.dto.ApiResponse;
import com.project.moviebooking.model.Booking;
import com.project.moviebooking.model.Ticket;
import com.project.moviebooking.service.BookingService;
import com.project.moviebooking.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * TicketController — serve ticket data to frontend
 * SOLID: S — only ticket retrieval, no other concerns
 */
@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class TicketController {

    private final BookingService     bookingService;
    private final JwtUtil            jwtUtil;
    private final UserProfileService userProfileService;

    /**
     * GET /api/tickets/{ticketId}
     * Get a ticket by its ID — used by Ticket page
     */
    @GetMapping("/{ticketId}")
    public ResponseEntity<ApiResponse<Ticket>> getTicket(@PathVariable String ticketId) {
        return ResponseEntity.ok(ApiResponse.success("Ticket found",
                bookingService.getTicketById(ticketId)));
    }

    /**
     * GET /api/tickets/booking/{bookingId}
     * Get ticket by booking ID
     */
    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<ApiResponse<Ticket>> getTicketByBooking(@PathVariable String bookingId) {
        return ResponseEntity.ok(ApiResponse.success("Ticket found",
                bookingService.getTicketByBookingId(bookingId)));
    }

    /**
     * GET /api/tickets/my
     * Get all tickets for logged-in user
     */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<Ticket>>> getMyTickets(
            @RequestHeader("Authorization") String authHeader) {
        String userId = extractUserId(authHeader);
        return ResponseEntity.ok(ApiResponse.success("Your tickets",
                bookingService.getTicketsByUser(userId)));
    }

    /**
     * GET /api/tickets/admin/all — admin only
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Ticket>>> getAllTickets() {
        // Reuse tickets via booking queries
        List<Booking> allBookings = bookingService.getAllBookings();
        return ResponseEntity.ok(ApiResponse.success(
                "Total bookings: " + allBookings.size(), null));
    }

    private String extractUserId(String authHeader) {
        String token = authHeader.substring(7);
        String email = jwtUtil.extractEmail(token);
        return userProfileService.getUserByEmail(email).getId();
    }
}
