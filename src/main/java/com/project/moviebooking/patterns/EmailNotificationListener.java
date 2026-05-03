package com.project.moviebooking.patterns;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * EmailNotificationListener — Observer (Concrete Observer 1)
 * ===========================================================
 * PATTERN: Observer (Behavioral)
 *
 * This is a CONCRETE OBSERVER that listens for BookingConfirmedEvent.
 * It is completely decoupled from the Publisher — Spring wires them.
 *
 * Adding this observer required ZERO changes to:
 * - BookingEventPublisher
 * - BookingServiceImpl
 * - PaymentService
 *
 * SOLID: O — open for new observers, closed for modification
 * ===========================================================
 */
@Component
public class EmailNotificationListener {

    /**
     * Spring automatically calls this when BookingConfirmedEvent is published.
     * @Async makes it non-blocking (runs in a separate thread)
     */
    @EventListener
    @Async
    public void onBookingConfirmed(BookingConfirmedEvent event) {
        System.out.println("\n📧 [EMAIL OBSERVER] Booking confirmed notification received:");
        System.out.println("   To:      " + event.getUserEmail());
        System.out.println("   Subject: Your CineBook ticket for " + event.getMovieTitle());
        System.out.println("   Body:    ─────────────────────────────────────");
        System.out.println("            🎬 " + event.getMovieTitle());
        System.out.println("            🎟️  Ticket Code: " + event.getTicketCode());
        System.out.println("            💺  Seats: " + event.getSeatNumbers());
        System.out.println("            💰  Amount Paid: ₹" + event.getAmount());
        System.out.println("            Booking ID: " + event.getBookingId());
        System.out.println("   ─────────────────────────────────────────────");
        System.out.println("✅ [EMAIL OBSERVER] Confirmation email sent (simulated)");
    }
}
