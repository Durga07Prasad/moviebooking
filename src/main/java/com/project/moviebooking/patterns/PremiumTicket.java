package com.project.moviebooking.patterns;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * PremiumTicket — Concrete ticket for rows C–E (premium seating)
 * ================================================================
 * SOLID: L — Substitutes AbstractTicket safely; 1.5x price is expected by contract
 * Factory Pattern: Created exclusively by TicketFactory
 * ================================================================
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PremiumTicket extends AbstractTicket {

    private static final String CATEGORY = "PREMIUM";

    public PremiumTicket() {}

    /**
     * Premium seats cost 1.5× base price
     * SOLID: L — returns valid double > 0, fulfils contract
     */
    @Override
    public double calculatePrice(double basePrice) {
        return basePrice * 1.5;  // 1.5x multiplier
    }

    @Override
    public String getTicketCategory() {
        return CATEGORY;
    }
}
