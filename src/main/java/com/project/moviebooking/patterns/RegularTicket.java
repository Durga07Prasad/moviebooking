package com.project.moviebooking.patterns;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * RegularTicket — Concrete ticket for rows F–J (standard seating)
 * ================================================================
 * SOLID: L — Substitutes AbstractTicket anywhere without breaking behaviour
 * Factory Pattern: Instantiated ONLY by TicketFactory, never directly
 *
 * LSP Test:
 *   AbstractTicket t = new RegularTicket();
 *   t.calculatePrice(150.0); // returns 150.0 — correct, no surprises
 * ================================================================
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RegularTicket extends AbstractTicket {

    private static final String CATEGORY = "REGULAR";

    public RegularTicket() {
        // Ticket code prefix set by TicketFactory
    }

    /**
     * Regular price — base price, no multiplier
     * SOLID: L — contract fulfilled: returns valid price > 0
     */
    @Override
    public double calculatePrice(double basePrice) {
        return basePrice;  // 1.0x multiplier
    }

    /**
     * SOLID: L — returns non-null String as contract demands
     */
    @Override
    public String getTicketCategory() {
        return CATEGORY;
    }
}
