package com.project.moviebooking.patterns;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * SMSNotificationListener — Observer (Concrete Observer 2)
 * ==========================================================
 * PATTERN: Observer (Behavioral)
 *
 * Second observer — added WITHOUT modifying BookingEventPublisher at all.
 * This proves OCP: the system is open for extension (new observers),
 * closed for modification (publisher code unchanged).
 * ==========================================================
 */
@Component
public class SMSNotificationListener {

    @EventListener
    @Async
    public void onBookingConfirmed(BookingConfirmedEvent event) {
        System.out.println("\n📱 [SMS OBSERVER] Sending SMS notification:");
        System.out.println("   To: User " + event.getUserId());
        System.out.println("   SMS: CineBook: Your booking for " +
                event.getMovieTitle() + " confirmed! " +
                "Code: " + event.getTicketCode() +
                " | Seats: " + event.getSeatNumbers() +
                " | Amt: ₹" + event.getAmount());
        System.out.println("✅ [SMS OBSERVER] SMS sent (simulated)");
    }
}
