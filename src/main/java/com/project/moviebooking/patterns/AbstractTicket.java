package com.project.moviebooking.patterns;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * AbstractTicket — Base class for Factory Pattern + Liskov Substitution Principle
 * ==================================================================================
 * DESIGN PATTERN: Factory Method (Creational)
 * SOLID: L — Liskov Substitution Principle
 *
 * Every concrete ticket (Regular, Premium, VIP) extends this.
 * Anywhere AbstractTicket is expected, any subtype substitutes safely.
 *
 * LSP guarantee:
 *   AbstractTicket t = ticketFactory.create(seatType, booking);
 *   t.getTicketCode();   ← works for ALL subtypes
 *   t.calculatePrice();  ← polymorphic, each type overrides correctly
 * ==================================================================================
 */
@Data
public abstract class AbstractTicket {

    protected String   bookingId;
    protected String   userId;
    protected String   userName;
    protected String   movieTitle;
    protected String   theatreName;
    protected String   showDate;
    protected String   showTime;
    protected List<String> seatNumbers;
    protected double   totalAmount;
    protected String   ticketCode;
    protected String   status = "VALID";
    protected LocalDateTime issuedAt = LocalDateTime.now();

    /**
     * Abstract method — each subtype defines its own price multiplier
     * SOLID: L — all subtypes honour this contract
     */
    public abstract double calculatePrice(double basePrice);

    /**
     * Each ticket type returns a human-readable category
     */
    public abstract String getTicketCategory();

    /**
     * Common to all tickets — SOLID: L — subclasses don't break this
     */
    public boolean isValid() {
        return "VALID".equals(this.status);
    }

    @Override
    public String toString() {
        return "[" + getTicketCategory() + "] " + ticketCode +
               " | " + movieTitle + " | Seats: " + seatNumbers +
               " | ₹" + totalAmount;
    }
}
