package com.project.moviebooking.patterns;

import com.project.moviebooking.model.Booking;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * TicketFactory — Factory Pattern (Creational)
 * =============================================
 * PURPOSE:
 *   Creates the correct AbstractTicket subtype based on seat type.
 *   Client code (BookingService) never uses `new RegularTicket()` directly.
 *   Factory hides all instantiation logic.
 *
 * PATTERN: Factory Method
 * PACKAGE: com.project.moviebooking.patterns
 *
 * SOLID — O (Open/Closed): To add a new ticket type (e.g., RECLINER),
 *   add `case "RECLINER" → new ReclineTicket()`.
 *   No existing class changes needed.
 *
 * SOLID — L (LSP): All returned types extend AbstractTicket.
 *   Callers receive AbstractTicket and can call any method safely.
 *
 * GRASP — Creator: BookingService has the Booking data needed to
 *   create an AbstractTicket, so it delegates to this Factory.
 * =============================================
 */
@Component
public class TicketFactory {

    /**
     * FACTORY METHOD — creates the right ticket type
     *
     * @param seatType  "REGULAR" | "PREMIUM" | "VIP"
     * @param booking   confirmed booking document
     * @param movieTitle   movie name for ticket display
     * @param theatreName  theatre name for ticket display
     * @param showDate     show date string
     * @param showTime     show time string
     * @param userName     ticket holder name
     * @return AbstractTicket (RegularTicket / PremiumTicket / VIPTicket)
     */
    public AbstractTicket createTicket(String seatType,
                                       Booking booking,
                                       String  movieTitle,
                                       String  theatreName,
                                       String  showDate,
                                       String  showTime,
                                       String  userName) {

        // ── FACTORY SWITCH — decides which class to instantiate ──
        AbstractTicket ticket = switch (seatType.toUpperCase()) {
            case "VIP"     -> {
                System.out.println("🏆 [FACTORY] Creating VIP ticket");
                yield new VIPTicket();
            }
            case "PREMIUM" -> {
                System.out.println("⭐ [FACTORY] Creating PREMIUM ticket");
                yield new PremiumTicket();
            }
            default        -> {
                System.out.println("🎟️ [FACTORY] Creating REGULAR ticket");
                yield new RegularTicket();
            }
        };

        // ── Populate common fields ──
        ticket.setBookingId(booking.getId());
        ticket.setUserId(booking.getUserId());
        ticket.setUserName(userName);
        ticket.setMovieTitle(movieTitle);
        ticket.setTheatreName(theatreName);
        ticket.setShowDate(showDate);
        ticket.setShowTime(showTime);
        ticket.setSeatNumbers(booking.getSeatNumbers());
        ticket.setTotalAmount(booking.getTotalAmount());
        ticket.setStatus("VALID");
        ticket.setIssuedAt(LocalDateTime.now());

        // ── Generate unique ticket code with type prefix ──
        String prefix = switch (seatType.toUpperCase()) {
            case "VIP"     -> "VIP";
            case "PREMIUM" -> "PRE";
            default        -> "REG";
        };
        ticket.setTicketCode(prefix + "-" +
                UUID.randomUUID().toString().replace("-", "")
                        .substring(0, 10).toUpperCase());

        System.out.println("✅ [FACTORY] Ticket created: " + ticket.getTicketCode() +
                " | Category: " + ticket.getTicketCategory() +
                " | Amount: ₹" + ticket.getTotalAmount());
        return ticket;
    }

    /**
     * Overloaded factory for seat list — determines dominant seat type
     */
    public AbstractTicket createTicket(List<String> seatNumbers,
                                       Booking booking,
                                       String movieTitle,
                                       String theatreName,
                                       String showDate,
                                       String showTime,
                                       String userName) {
        String seatType = determineSeatType(seatNumbers);
        return createTicket(seatType, booking, movieTitle,
                theatreName, showDate, showTime, userName);
    }

    /**
     * Determine dominant seat type from seat number prefixes
     * Rows A–B → VIP, C–E → PREMIUM, F–J → REGULAR
     */
    public String determineSeatType(List<String> seatNumbers) {
        if (seatNumbers == null || seatNumbers.isEmpty()) return "REGULAR";
        long vip     = seatNumbers.stream()
                .filter(s -> s.matches("^[AB].*")).count();
        long premium = seatNumbers.stream()
                .filter(s -> s.matches("^[CDE].*")).count();
        if (vip > 0 && vip >= premium) return "VIP";
        if (premium > 0)               return "PREMIUM";
        return "REGULAR";
    }
}
