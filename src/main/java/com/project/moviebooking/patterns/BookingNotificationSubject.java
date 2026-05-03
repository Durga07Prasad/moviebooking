package com.project.moviebooking.patterns;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * ===================================================
 * DESIGN PATTERN 3: OBSERVER PATTERN (Behavioral)
 * ===================================================
 * Purpose: Define a one-to-many dependency so that when
 *          ONE object (Subject) changes state, ALL its
 *          dependents (Observers) are notified automatically.
 *
 * Real-world use here:
 * - When a booking is CONFIRMED → notify user via Email
 * - When a booking is CONFIRMED → notify user via SMS
 * - When a ticket is ISSUED → send ticket to Email
 *
 * Components:
 * 1. BookingObserver (Observer Interface) - what all observers must implement
 * 2. EmailNotificationObserver - sends email
 * 3. SMSNotificationObserver - sends SMS
 * 4. BookingSubject - the Subject that notifies observers
 *
 * SOLID connection:
 * - O: Open/Closed - add new notification type without changing Subject
 * - D: Dependency Inversion - Subject depends on abstraction (BookingObserver)
 * ===================================================
 */

// ---- 1. OBSERVER INTERFACE ----
interface BookingObserver {
    void onBookingConfirmed(String userId, String bookingId, String movieTitle, double amount);
    void onBookingCancelled(String userId, String bookingId);
    void onTicketIssued(String userId, String ticketCode, String movieTitle);
}

// ---- 2. CONCRETE OBSERVER: Email Notification ----
class EmailNotificationObserver implements BookingObserver {

    @Override
    public void onBookingConfirmed(String userId, String bookingId, String movieTitle, double amount) {
        // In production: use JavaMailSender to send real emails
        System.out.println("📧 [EMAIL] Booking confirmed!");
        System.out.println("   → User: " + userId);
        System.out.println("   → Booking ID: " + bookingId);
        System.out.println("   → Movie: " + movieTitle);
        System.out.println("   → Amount Paid: ₹" + amount);
    }

    @Override
    public void onBookingCancelled(String userId, String bookingId) {
        System.out.println("📧 [EMAIL] Booking cancelled - Refund initiated for booking: " + bookingId);
    }

    @Override
    public void onTicketIssued(String userId, String ticketCode, String movieTitle) {
        System.out.println("📧 [EMAIL] Your e-ticket is ready!");
        System.out.println("   → Ticket Code: " + ticketCode);
        System.out.println("   → Show this at the theatre entrance");
    }
}

// ---- 3. CONCRETE OBSERVER: SMS Notification ----
class SMSNotificationObserver implements BookingObserver {

    @Override
    public void onBookingConfirmed(String userId, String bookingId, String movieTitle, double amount) {
        // In production: use Twilio SMS API
        System.out.println("📱 [SMS] BOOKING CONFIRMED! Movie: " + movieTitle + " | ₹" + amount + " paid.");
    }

    @Override
    public void onBookingCancelled(String userId, String bookingId) {
        System.out.println("📱 [SMS] Your booking " + bookingId + " has been cancelled. Refund in 3-5 days.");
    }

    @Override
    public void onTicketIssued(String userId, String ticketCode, String movieTitle) {
        System.out.println("📱 [SMS] E-Ticket: " + ticketCode + " | Enjoy " + movieTitle + "!");
    }
}

// ---- 4. CONCRETE OBSERVER: Push Notification ----
class PushNotificationObserver implements BookingObserver {

    @Override
    public void onBookingConfirmed(String userId, String bookingId, String movieTitle, double amount) {
        System.out.println("🔔 [PUSH] Booking Confirmed! Your seats are reserved for " + movieTitle);
    }

    @Override
    public void onBookingCancelled(String userId, String bookingId) {
        System.out.println("🔔 [PUSH] Booking Cancelled - check your email for refund details.");
    }

    @Override
    public void onTicketIssued(String userId, String ticketCode, String movieTitle) {
        System.out.println("🔔 [PUSH] Your ticket " + ticketCode + " is ready! Tap to view.");
    }
}

/**
 * SUBJECT: BookingNotificationSubject
 * Manages observers and notifies them on events
 */
@Component
public class BookingNotificationSubject {

    // List of all registered observers
    private final List<BookingObserver> observers = new ArrayList<>();

    // Register default observers at startup
    public BookingNotificationSubject() {
        registerObserver(new EmailNotificationObserver());
        registerObserver(new SMSNotificationObserver());
        registerObserver(new PushNotificationObserver());
        System.out.println("✅ [OBSERVER] Notification system initialized with " + observers.size() + " observers");
    }

    /**
     * Register a new observer (open for extension)
     */
    public void registerObserver(BookingObserver observer) {
        observers.add(observer);
    }

    /**
     * Remove an observer
     */
    public void removeObserver(BookingObserver observer) {
        observers.remove(observer);
    }

    /**
     * Notify all observers: booking confirmed
     */
    public void notifyBookingConfirmed(String userId, String bookingId, String movieTitle, double amount) {
        System.out.println("\n📣 Broadcasting BOOKING_CONFIRMED event to " + observers.size() + " observers...");
        for (BookingObserver observer : observers) {
            observer.onBookingConfirmed(userId, bookingId, movieTitle, amount);
        }
    }

    /**
     * Notify all observers: booking cancelled
     */
    public void notifyBookingCancelled(String userId, String bookingId) {
        System.out.println("\n📣 Broadcasting BOOKING_CANCELLED event to " + observers.size() + " observers...");
        for (BookingObserver observer : observers) {
            observer.onBookingCancelled(userId, bookingId);
        }
    }

    /**
     * Notify all observers: ticket issued
     */
    public void notifyTicketIssued(String userId, String ticketCode, String movieTitle) {
        System.out.println("\n📣 Broadcasting TICKET_ISSUED event to " + observers.size() + " observers...");
        for (BookingObserver observer : observers) {
            observer.onTicketIssued(userId, ticketCode, movieTitle);
        }
    }
}
