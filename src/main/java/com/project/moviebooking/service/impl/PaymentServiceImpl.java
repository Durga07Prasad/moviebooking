package com.project.moviebooking.service.impl;

import com.project.moviebooking.dto.ApiResponse;
import com.project.moviebooking.model.*;
import com.project.moviebooking.patterns.*;
import com.project.moviebooking.repository.*;
import com.project.moviebooking.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * PaymentServiceImpl — GST calculation + Strategy pattern + Refund logic
 * =======================================================================
 * GST Rules (Indian Cinema Tax — as per GOI circular):
 *   Base ticket price > ₹100 → 18% GST
 *   Base ticket price ≤ ₹100 → 12% GST
 *
 * Refund Rules (BookMyShow-style policy):
 *   Cancelled > 2h before show → 50% refund of BASE price (not GST)
 *   Cancelled ≤ 2h before show → No refund
 *
 * Design Patterns used here:
 *   STRATEGY  — PaymentContext selects UPI/Card/Wallet at runtime
 *   ADAPTER   — PaymentGatewayAdapter bridges ExternalPaymentAPI
 *   OBSERVER  — BookingEventPublisher fires after payment success
 * =======================================================================
 */
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository  paymentRepository;
    private final BookingRepository  bookingRepository;
    private final TicketRepository   ticketRepository;
    private final ShowRepository     showRepository;
    private final PaymentContext     paymentContext;       // STRATEGY Pattern
    private final PaymentGatewayAdapter gatewayAdapter;   // ADAPTER Pattern

    @Value("${gst.rate.high:18.0}")
    private double gstHigh;

    @Value("${gst.rate.low:12.0}")
    private double gstLow;

    @Value("${gst.threshold:100.0}")
    private double gstThreshold;

    // ─────────────────────────────────────────────────────────────────
    // PROCESS PAYMENT — GST calculation + Strategy execution
    // ─────────────────────────────────────────────────────────────────
    @Override
    public Map<String, Object> processPayment(String bookingId, String paymentMethod,
                                               String userId, String upiId, String cardNumber) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));

        if (!"PENDING".equals(booking.getBookingStatus()))
            throw new RuntimeException("Booking is " + booking.getBookingStatus() + " — cannot process payment.");

        double baseAmount = booking.getTotalAmount();

        // ── GST CALCULATION ──────────────────────────────────────────
        double gstPercent = (baseAmount > gstThreshold) ? gstHigh : gstLow;
        double gstAmount  = Math.round(baseAmount * (gstPercent / 100.0) * 100.0) / 100.0;
        double totalAmount = Math.round((baseAmount + gstAmount) * 100.0) / 100.0;

        System.out.printf("💰 [GST] Base: ₹%.2f | GST %.0f%%: ₹%.2f | Total: ₹%.2f%n",
                baseAmount, gstPercent, gstAmount, totalAmount);

        // ── STRATEGY PATTERN: select payment method ───────────────────
        paymentContext.setStrategy(paymentMethod);

        // ── ADAPTER PATTERN: call external gateway ────────────────────
        String transactionId = gatewayAdapter.processPayment(userId, totalAmount, paymentMethod);

        // ── SAVE PAYMENT with full GST breakdown ─────────────────────
        Payment payment = new Payment();
        payment.setBookingId(bookingId);
        payment.setUserId(userId);
        payment.setBaseAmount(baseAmount);
        payment.setGstPercent(gstPercent);
        payment.setGstAmount(gstAmount);
        payment.setTotalAmount(totalAmount);
        payment.setPaymentMethod(paymentMethod);
        payment.setStatus("SUCCESS");
        payment.setTransactionId(transactionId);
        payment.setRefundStatus("NONE");
        payment.setPaymentTime(LocalDateTime.now());

        Payment savedPayment = paymentRepository.save(payment);

        // ── Result map with GST breakdown ──────────────────────────────
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("payment", savedPayment);
        result.put("gstBreakdown", buildGstBreakdown(baseAmount, gstPercent, gstAmount, totalAmount));

        return result;
    }

    // ─────────────────────────────────────────────────────────────────
    // CANCEL PAYMENT / REFUND — BookMyShow-style 50% refund rule
    // ─────────────────────────────────────────────────────────────────
    @Override
    public Map<String, Object> cancelAndRefund(String bookingId, String userId) {
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new RuntimeException("Payment not found for booking: " + bookingId));

        Show show = showRepository.findById(
                bookingRepository.findById(bookingId)
                        .orElseThrow(() -> new RuntimeException("Booking not found"))
                        .getShowId()
        ).orElseThrow(() -> new RuntimeException("Show not found"));

        // ── REFUND ELIGIBILITY CHECK ───────────────────────────────────
        LocalDateTime showDateTime = show.getShowDate().atTime(show.getShowTime());
        LocalDateTime now = LocalDateTime.now();
        long hoursUntilShow = java.time.Duration.between(now, showDateTime).toHours();

        double refundAmount = 0;
        String refundStatus;
        String refundMessage;

        if (hoursUntilShow > 2) {
            // ✅ More than 2 hours before show → 50% refund of BASE price (not GST)
            refundAmount = Math.round(payment.getBaseAmount() * 0.5 * 100.0) / 100.0;
            refundStatus = "PROCESSED";
            refundMessage = String.format(
                "✅ Refund approved! ₹%.2f (50%% of base ₹%.2f) will be credited in 3-5 business days.",
                refundAmount, payment.getBaseAmount());

            payment.setRefundAmount(refundAmount);
            payment.setRefundStatus("PROCESSED");
            payment.setStatus("REFUNDED");
            payment.setRefundReason("Cancelled " + hoursUntilShow + " hours before show (>2h policy)");
        } else if (hoursUntilShow > 0) {
            // ❌ Within 2 hours of show — no refund
            refundStatus = "NOT_ELIGIBLE";
            refundMessage = String.format(
                "❌ No refund. Show starts in %d hour(s). Refunds only available >2 hours before showtime.",
                hoursUntilShow);

            payment.setRefundStatus("NOT_ELIGIBLE");
            payment.setRefundReason("Cancelled within 2 hours of showtime — no refund policy");
        } else {
            // Show has already passed
            refundStatus = "NOT_ELIGIBLE";
            refundMessage = "❌ No refund. This show has already started or passed.";
            payment.setRefundStatus("NOT_ELIGIBLE");
            payment.setRefundReason("Show already passed");
        }

        paymentRepository.save(payment);

        // ── Build response ─────────────────────────────────────────────
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("bookingId",     bookingId);
        result.put("refundAmount",  refundAmount);
        result.put("refundStatus",  refundStatus);
        result.put("message",       refundMessage);
        result.put("baseAmount",    payment.getBaseAmount());
        result.put("gstAmount",     payment.getGstAmount());
        result.put("totalPaid",     payment.getTotalAmount());
        result.put("hoursBeforeShow", hoursUntilShow);
        result.put("gstBreakdown",  "GST (₹" + payment.getGstAmount() + ") is non-refundable per Indian tax law");

        return result;
    }

    @Override
    public Payment getPaymentByBookingId(String bookingId) {
        return paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
    }

    // ── GST Breakdown helper ──────────────────────────────────────────
    private Map<String, Object> buildGstBreakdown(double base, double gstPct, double gst, double total) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("displayText",  String.format(
            "Base Price: ₹%.2f | GST (%.0f%%): ₹%.2f | Total: ₹%.2f", base, gstPct, gst, total));
        m.put("basePrice",    "₹" + base);
        m.put("gstRate",      gstPct + "% (Indian Cinema Tax)");
        m.put("gstAmount",    "₹" + gst);
        m.put("totalPayable", "₹" + total);
        m.put("note",         gstPct == 18.0
            ? "Tickets above ₹100 attract 18% GST as per Indian tax law"
            : "Tickets ≤ ₹100 attract 12% GST as per Indian tax law");
        return m;
    }
}
