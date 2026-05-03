package com.project.moviebooking.patterns;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * VIPTicket — Concrete ticket for rows A–B (VIP lounge seating)
 * ================================================================
 * SOLID: L — Substitutes AbstractTicket safely; 2x price is expected by contract
 * Factory Pattern: Created exclusively by TicketFactory
 *
 * LSP Polymorphism Demo (used in report):
 *   List<AbstractTicket> tickets = List.of(
 *       new RegularTicket(), new PremiumTicket(), new VIPTicket()
 *   );
 *   tickets.forEach(t -> System.out.println(
 *       t.getTicketCategory() + " → ₹" + t.calculatePrice(150)
 *   ));
 *   // Output:
 *   // REGULAR  → ₹150.0
 *   // PREMIUM  → ₹225.0
 *   // VIP      → ₹300.0
 * ================================================================
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class VIPTicket extends AbstractTicket {

    private static final String CATEGORY = "VIP";

    public VIPTicket() {}

    /**
     * VIP seats cost 2× base price
     * SOLID: L — postcondition met: price > base, valid for all callers
     */
    @Override
    public double calculatePrice(double basePrice) {
        return basePrice * 2.0;  // 2.0x multiplier
    }

    @Override
    public String getTicketCategory() {
        return CATEGORY;
    }
}
