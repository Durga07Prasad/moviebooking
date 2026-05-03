package com.project.moviebooking.controller;

import com.project.moviebooking.config.JwtUtil;
import com.project.moviebooking.dto.ApiResponse;
import com.project.moviebooking.dto.BookingRequest;
import com.project.moviebooking.model.*;
import com.project.moviebooking.repository.*;
import com.project.moviebooking.service.ISTTimeService;
import com.project.moviebooking.service.RefundService;
import com.project.moviebooking.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * BookingController — IST-aware atomic seat booking + refund-aware cancellation.
 *
 * OOAD: GRASP Controller — all HTTP events handled here, delegated to services.
 * SOLID: D  — depends on service interfaces and ISTTimeService, never on impls.
 * SOLID: S  — only booking lifecycle HTTP concerns (create, cancel, view).
 *
 * ATOMIC BOOKING: all-or-nothing seat locking — if ANY seat in the request
 * is already booked, the ENTIRE booking fails. No partial reservations.
 */
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000","http://localhost:3001","http://localhost:5173"})
public class BookingController {

    private final BookingRepository  bookingRepository;
    private final ShowRepository     showRepository;
    private final SeatRepository     seatRepository;
    private final TicketRepository   ticketRepository;
    private final PaymentRepository  paymentRepository;
    private final UserProfileService userProfileService;
    private final ISTTimeService     ist;
    private final RefundService      refundService;
    private final JwtUtil            jwtUtil;

    // ─────────────────────────────────────────────────────────────────────────
    /**
     * POST /api/bookings
     * Body: { showId, seatNumbers[] }
     *
     * OOAD: GRASP Creator — BookingController creates Booking because it
     *       aggregates Show + Seat + User data to build the booking.
     * SOLID: S — HTTP concern only; seat-level logic handled via repositories.
     *
     * Steps:
     *   1. IST check — reject if show is not bookable
     *   2. findByShowIdAndSeatNumberIn — bulk fetch requested seats
     *   3. Verify seat count matches request
     *   4. Collect already-booked seats — reject with exact seat numbers
     *   5. Atomic lock: isBooked=true + bookedByUserId on ALL seats
     *   6. Decrement show.availableSeats
     *   7. Build Booking with PENDING status + IST bookingTime
     *   8. Return saved booking
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Booking>> createBooking(
            @RequestBody Map<String, Object> body,
            HttpServletRequest httpRequest) {

        String userId = extractUserId(httpRequest);

        String showId = (String) body.get("showId");
        @SuppressWarnings("unchecked")
        List<String> seatNumbers = (List<String>) body.get("seatNumbers");

        if (showId == null || seatNumbers == null || seatNumbers.isEmpty())
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("showId and seatNumbers are required."));

        if (seatNumbers.size() > 8)
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Maximum 8 seats allowed per booking."));

        // ── Step 1: IST Bookability Check ─────────────────────────────────
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new RuntimeException("Show not found: " + showId));

        if (!ist.isShowBookable(show.getShowDate().toString(), show.getShowTime().toString())) {
            return ResponseEntity.badRequest().body(ApiResponse.error(
                "This show is no longer bookable. " +
                "Show time: " + ist.formatTo12Hour(show.getShowTime().toString()) +
                " | Current IST: " + ist.currentISTString()));
        }

        // ── Step 1b: Auto-void stale PENDING bookings for same show ───────
        // Prevents seat hoarding if user navigated away without paying.
        List<Booking> stalePending = bookingRepository
                .findByUserIdAndShowIdAndBookingStatus(userId, showId, "PENDING");
        if (!stalePending.isEmpty()) {
            System.out.println("🧹 [BOOKING] Voiding " + stalePending.size() + " stale PENDING booking(s)");
            stalePending.forEach(old -> {
                // Release stale seats
                List<Seat> staleSeats = seatRepository.findByShowIdAndSeatNumberIn(
                        old.getShowId(), old.getSeatNumbers());
                staleSeats.forEach(s -> { s.setBooked(false); s.setBookedByUserId(null); });
                seatRepository.saveAll(staleSeats);
                // Restore show count
                show.setAvailableSeats(show.getAvailableSeats() + old.getNumberOfSeats());
                show.getBookedSeats().removeAll(old.getSeatNumbers());
                // Cancel the stale booking
                old.setBookingStatus("CANCELLED");
                bookingRepository.save(old);
            });
            showRepository.save(show);
        }

        // ── Step 2: Bulk fetch seats ───────────────────────────────────────
        List<Seat> seats = seatRepository.findByShowIdAndSeatNumberIn(showId, seatNumbers);

        // ── Step 3: Seat count validation ──────────────────────────────────
        if (seats.size() != seatNumbers.size()) {
            List<String> found = seats.stream().map(Seat::getSeatNumber).toList();
            List<String> missing = seatNumbers.stream().filter(s -> !found.contains(s)).toList();
            return ResponseEntity.badRequest().body(ApiResponse.error(
                "Seats not found in this show: " + missing));
        }

        // ── Step 4: Check for already-booked seats ─────────────────────────
        List<String> alreadyBooked = seats.stream()
                .filter(Seat::isBooked)
                .map(Seat::getSeatNumber)
                .toList();

        if (!alreadyBooked.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error(
                "Seats already booked: " + alreadyBooked +
                ". Please choose different seats."));
        }

        // ── Step 5: Atomic seat lock ──────────────────────────────────────
        seats.forEach(seat -> {
            seat.setBooked(true);
            seat.setBookedByUserId(userId);
        });
        seatRepository.saveAll(seats);
        System.out.println("🔒 [BOOKING] Locked " + seats.size() + " seats for user: " + userId);

        // ── Step 6: Decrement available seats ─────────────────────────────
        show.setAvailableSeats(Math.max(0, show.getAvailableSeats() - seats.size()));
        show.getBookedSeats().addAll(seatNumbers);
        showRepository.save(show);

        // ── Step 7: Calculate total + build Booking ───────────────────────
        double total = seats.stream().mapToDouble(Seat::getPrice).sum();

        Booking booking = new Booking();
        booking.setUserId(userId);
        booking.setShowId(showId);
        booking.setMovieId(show.getMovieId());
        booking.setTheatreId(show.getTheatreId());
        booking.setSeatNumbers(seatNumbers);
        booking.setNumberOfSeats(seatNumbers.size());
        booking.setTotalAmount(total);
        booking.setBookingStatus("PENDING");
        booking.setBookingTime(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));

        // ── Step 8: Save and return ───────────────────────────────────────
        Booking saved = bookingRepository.save(booking);
        System.out.println("✅ [BOOKING] Created PENDING booking: " + saved.getId());

        return ResponseEntity.ok(ApiResponse.success(
                "Seats reserved! Proceed to payment.", saved));
    }

    // ─────────────────────────────────────────────────────────────────────────
    /**
     * PUT /api/bookings/{id}/cancel  (also POST for frontend convenience)
     *
     * Cancels a booking, releases seats, calculates IST-aware refund.
     *
     * OOAD: GRASP Information Expert — RefundService decides refund eligibility.
     * SOLID: S — cancellation orchestration only; no refund math here.
     *
     * Steps:
     *   1. Verify booking belongs to current user (403 if not)
     *   2. Check not already cancelled
     *   3. Fetch show + payment
     *   4. Calculate refund via RefundService
     *   5. Release all seats (isBooked=false, bookedByUserId=null)
     *   6. Restore show.availableSeats
     *   7. Set booking.bookingStatus = CANCELLED
     *   8. Cancel ticket if exists
     *   9. Update payment with refund fields + IST refundTime
     *   10. Return full cancellation summary map
     */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Map<String,Object>>> cancelBooking(
            @PathVariable String id,
            HttpServletRequest httpRequest) {

        String userId = extractUserId(httpRequest);

        // ── Step 1: Ownership check ────────────────────────────────────────
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found: " + id));

        if (!booking.getUserId().equals(userId)) {
            return ResponseEntity.status(403).body(ApiResponse.error(
                "Forbidden: this booking does not belong to you."));
        }

        // ── Step 2: Already-cancelled guard ───────────────────────────────
        if ("CANCELLED".equals(booking.getBookingStatus())) {
            return ResponseEntity.badRequest().body(ApiResponse.error(
                "Booking is already cancelled."));
        }

        // ── Step 3: Fetch show + check payment status ─────────────────────
        Show show = showRepository.findById(booking.getShowId())
                .orElseThrow(() -> new RuntimeException("Show not found"));

        Optional<Payment> paymentOpt = paymentRepository.findByBookingId(id);

        // Payment is considered "made" only when status is SUCCESS (set by PaymentServiceImpl)
        boolean paymentWasMade = paymentOpt.isPresent()
                && "SUCCESS".equals(paymentOpt.get().getStatus());

        // ── Step 4a: PENDING booking (no payment) — just void it ──────────
        if (!paymentWasMade) {
            System.out.println("🗑️ [CANCEL] No payment found — voiding PENDING booking: " + id);

            // Release seats
            List<Seat> seats = seatRepository.findByShowIdAndSeatNumberIn(
                    booking.getShowId(), booking.getSeatNumbers());
            seats.forEach(s -> { s.setBooked(false); s.setBookedByUserId(null); });
            seatRepository.saveAll(seats);

            // Restore show count
            show.setAvailableSeats(show.getAvailableSeats() + booking.getNumberOfSeats());
            show.getBookedSeats().removeAll(booking.getSeatNumbers());
            showRepository.save(show);

            // Mark cancelled
            booking.setBookingStatus("CANCELLED");
            bookingRepository.save(booking);

            // Clean response — no refund mention
            Map<String,Object> voidResponse = new LinkedHashMap<>();
            voidResponse.put("bookingId",      id);
            voidResponse.put("status",         "CANCELLED");
            voidResponse.put("paymentWasMade", false);
            voidResponse.put("refundEligible", false);
            voidResponse.put("refundAmount",   0);
            voidResponse.put("seatsReleased",  seats.stream().map(Seat::getSeatNumber).toList());
            voidResponse.put("message",
                    "Booking voided. No payment was made, so no refund is needed.");
            voidResponse.put("cancelledAtIST", ist.currentISTString());

            return ResponseEntity.ok(ApiResponse.success("Booking voided.", voidResponse));
        }

        // ── Step 4b: CONFIRMED booking (payment made) — calculate refund ───
        Payment payment = paymentOpt.get();
        double basePaid = payment.getBaseAmount();
        RefundService.RefundResult refund = refundService.calculate(
                basePaid,
                show.getShowDate().toString(),
                show.getShowTime().toString(),
                ist);

        System.out.println("💰 [CANCEL+REFUND] " + refund);

        // ── Step 5: Release seats ──────────────────────────────────────────
        List<Seat> seatsToRelease = seatRepository.findByShowIdAndSeatNumberIn(
                booking.getShowId(), booking.getSeatNumbers());
        seatsToRelease.forEach(seat -> {
            seat.setBooked(false);
            seat.setBookedByUserId(null);
        });
        seatRepository.saveAll(seatsToRelease);

        // ── Step 6: Restore show available count ──────────────────────────
        show.setAvailableSeats(show.getAvailableSeats() + seatsToRelease.size());
        show.getBookedSeats().removeAll(booking.getSeatNumbers());
        showRepository.save(show);

        // ── Step 7: Mark booking CANCELLED ────────────────────────────────
        booking.setBookingStatus("CANCELLED");
        bookingRepository.save(booking);

        // ── Step 8: Cancel ticket if exists ───────────────────────────────
        ticketRepository.findByBookingId(id).ifPresent(ticket -> {
            ticket.setStatus("CANCELLED");
            ticketRepository.save(ticket);
        });

        // ── Step 9: Update payment refund fields ───────────────────────────
        payment.setRefundAmount(refund.refundAmount);
        payment.setRefundStatus(refund.refundStatus);
        payment.setRefundReason(refund.reason);
        payment.setRefundTime(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
        if (refund.eligible) payment.setStatus("REFUNDED");
        paymentRepository.save(payment);

        // ── Step 10: Build response map ───────────────────────────────────
        Map<String,Object> response = new LinkedHashMap<>();
        response.put("bookingId",       id);
        response.put("status",          "CANCELLED");
        response.put("paymentWasMade",  true);
        response.put("seatsReleased",   seatsToRelease.stream().map(Seat::getSeatNumber).toList());
        response.put("refundEligible",  refund.eligible);
        response.put("refundAmount",    refund.refundAmount);
        response.put("refundStatus",    refund.refundStatus);
        response.put("refundReason",    refund.reason);
        response.put("basePaid",        basePaid);
        response.put("gstPaid",         payment.getGstAmount());
        response.put("gstNote",         "GST (₹" + payment.getGstAmount() + ") is non-refundable per Indian tax regulation");
        response.put("message",         refund.eligible
                ? "✅ Booking cancelled. ₹" + refund.refundAmount + " will be credited in 5–7 business days."
                : "❌ Booking cancelled. " + refund.reason);
        response.put("cancelledAtIST",  ist.currentISTString());

        return ResponseEntity.ok(ApiResponse.success("Booking cancelled.", response));
    }

    /** POST mirror of cancel for frontend convenience */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Map<String,Object>>> cancelBookingPost(
            @PathVariable String id, HttpServletRequest req) {
        return cancelBooking(id, req);
    }

    // ─────────────────────────────────────────────────────────────────────────
    /**
     * GET /api/bookings/my
     * Returns current user's bookings newest-first.
     * SOLID: S — read-only concern, no mutation.
     */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<Booking>>> getMyBookings(HttpServletRequest req) {
        String userId = extractUserId(req);
        return ResponseEntity.ok(ApiResponse.success("Your bookings",
                bookingRepository.findByUserIdOrderByBookingTimeDesc(userId)));
    }

    /**
     * GET /api/bookings/{id} — single booking by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Booking>> getBooking(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Booking",
                bookingRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Booking not found: " + id))));
    }

    /**
     * GET /api/bookings/{bookingId}/ticket — ticket for a booking
     */
    @GetMapping("/{bookingId}/ticket")
    public ResponseEntity<ApiResponse<Ticket>> getTicket(@PathVariable String bookingId) {
        return ResponseEntity.ok(ApiResponse.success("Ticket",
                ticketRepository.findByBookingId(bookingId)
                        .orElseThrow(() -> new RuntimeException("Ticket not found"))));
    }

    // ── Helper: extract userId from JWT ─────────────────────────────────────
    /**
     * GRASP: Controller helper — JWT extraction stays in the controller layer.
     * SOLID: S — authentication extraction is separate from booking logic.
     */
    private String extractUserId(HttpServletRequest req) {
        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer "))
            throw new RuntimeException("Missing or invalid Authorization header");
        String email = jwtUtil.extractEmail(header.substring(7));
        return userProfileService.getUserByEmail(email).getId();
    }
}
