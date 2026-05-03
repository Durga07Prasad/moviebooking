package com.project.moviebooking.patterns;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * BookingEventPublisher — Observer Pattern SUBJECT (Publisher)
 * =============================================================
 * PATTERN: Observer (Behavioral) — via Spring ApplicationEventPublisher
 * PACKAGE: com.project.moviebooking.patterns
 *
 * ROLE: Publishes BookingConfirmedEvent to Spring's event bus.
 *       All registered @EventListener methods receive it automatically.
 *
 * WHY SPRING EVENTS?
 *   - Low coupling: Publisher doesn't know who the observers are
 *   - New observer = new @EventListener class, no changes here
 *   - OCP compliant: Open for new listeners, closed for modification
 *
 * COMPARISON WITH MANUAL OBSERVER:
 *   Manual:  subject.addObserver(emailObs); subject.notifyAll();
 *   Spring:  publisher.publishEvent(event); // Spring handles the rest
 *
 * =============================================================
 */
@Component
@RequiredArgsConstructor
public class BookingEventPublisher {

    // SOLID: D — depends on ApplicationEventPublisher (Spring abstraction)
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Publish booking confirmed event to all registered observers
     *
     * Called by: BookingServiceImpl.confirmBookingAndIssueTicket()
     * Observed by: EmailNotificationListener, SMSNotificationListener
     */
    public void publishBookingConfirmed(String bookingId,
                                        String userId,
                                        String userEmail,
                                        String movieTitle,
                                        String ticketCode,
                                        double amount,
                                        String seatNumbers) {

        System.out.println("\n📢 [OBSERVER] Publishing BookingConfirmedEvent...");
        System.out.println("   BookingID: " + bookingId + " | Movie: " + movieTitle);

        BookingConfirmedEvent event = new BookingConfirmedEvent(
                this, bookingId, userId, userEmail,
                movieTitle, ticketCode, amount, seatNumbers
        );

        // Spring delivers this to ALL @EventListener(BookingConfirmedEvent.class) methods
        eventPublisher.publishEvent(event);

        System.out.println("✅ [OBSERVER] Event published — Spring will notify all listeners");
    }
}
