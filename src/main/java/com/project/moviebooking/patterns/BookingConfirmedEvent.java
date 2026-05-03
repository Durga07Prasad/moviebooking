package com.project.moviebooking.patterns;

import org.springframework.context.ApplicationEvent;

/**
 * BookingConfirmedEvent — Spring Application Event (Observer Pattern)
 * ====================================================================
 * PATTERN: Observer (Behavioral)
 * MECHANISM: Spring ApplicationEventPublisher (built-in Spring Observer)
 *
 * This event is PUBLISHED when a booking is confirmed after payment.
 * OBSERVERS (Listeners) receive this event without being directly coupled
 * to the publisher (BookingEventPublisher).
 *
 * ┌──────────────────────┐  publishes  ┌─────────────────────────┐
 * │ BookingEventPublisher│────────────▶│  BookingConfirmedEvent  │
 * └──────────────────────┘             └─────────────┬───────────┘
 *                                                    │ delivered by Spring
 *                                       ┌────────────┴──────────────┐
 *                               ┌───────▼────────┐   ┌─────────────▼────────┐
 *                               │ EmailListener  │   │  SMSListener         │
 *                               │ (@EventListener)│  │  (@EventListener)    │
 *                               └────────────────┘   └──────────────────────┘
 * ====================================================================
 */
public class BookingConfirmedEvent extends ApplicationEvent {

    private final String bookingId;
    private final String userId;
    private final String userEmail;
    private final String movieTitle;
    private final String ticketCode;
    private final double amount;
    private final String seatNumbers;

    public BookingConfirmedEvent(Object source,
                                  String bookingId,
                                  String userId,
                                  String userEmail,
                                  String movieTitle,
                                  String ticketCode,
                                  double amount,
                                  String seatNumbers) {
        super(source);
        this.bookingId   = bookingId;
        this.userId      = userId;
        this.userEmail   = userEmail;
        this.movieTitle  = movieTitle;
        this.ticketCode  = ticketCode;
        this.amount      = amount;
        this.seatNumbers = seatNumbers;
    }

    // Getters
    public String getBookingId()   { return bookingId;   }
    public String getUserId()      { return userId;      }
    public String getUserEmail()   { return userEmail;   }
    public String getMovieTitle()  { return movieTitle;  }
    public String getTicketCode()  { return ticketCode;  }
    public double getAmount()      { return amount;      }
    public String getSeatNumbers() { return seatNumbers; }
}
