package com.project.moviebooking.controller;

import com.project.moviebooking.dto.ApiResponse;
import com.project.moviebooking.model.Booking;
import com.project.moviebooking.model.Ticket;
import com.project.moviebooking.service.BookingService;
import com.project.moviebooking.service.PaymentService;
import com.project.moviebooking.config.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * PaymentController — Payment processing + GST breakdown + Refund
 * ================================================================
 * SOLID: D — depends on PaymentService interface (not impl)
 * GRASP: Controller — zero business logic, pure HTTP delegation
 * ================================================================
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000","http://localhost:3001","http://localhost:5173"})
public class PaymentController {

    private final PaymentService  paymentService;
    private final BookingService  bookingService;
    private final JwtUtil         jwtUtil;

    /**
     * POST /api/payments/process
     * Request: { bookingId, paymentMethod, upiId?, cardNumber? }
     * Response: { payment (with GST breakdown), ticket }
     */
    @PostMapping("/process")
    public ResponseEntity<ApiResponse<Map<String,Object>>> processPayment(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {

        String userId    = extractUserId(httpRequest);
        String userEmail = extractUserEmail(httpRequest);

        String bookingId     = request.get("bookingId");
        String paymentMethod = request.getOrDefault("paymentMethod", "UPI").toUpperCase();
        String upiId         = request.get("upiId");
        String cardNumber    = request.get("cardNumber");

        // 1. Process payment with GST
        Map<String, Object> paymentResult = paymentService.processPayment(
                bookingId, paymentMethod, userId, upiId, cardNumber);

        // 2. Confirm booking + generate ticket (via BookingService)
        Booking confirmedBooking = bookingService.getBookingById(bookingId);
        Ticket ticket = bookingService.confirmBookingAndIssueTicket(
                bookingId,
                ((com.project.moviebooking.model.Payment) paymentResult.get("payment")).getId(),
                userEmail);

        paymentResult.put("ticket",  ticket);
        paymentResult.put("booking", confirmedBooking);

        return ResponseEntity.ok(ApiResponse.success(
            "✅ Payment successful! GST applied. Ticket generated.", paymentResult));
    }

    /**
     * GET /api/payments/my
     * Current user's payment history
     */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<Object>> getMyPayments(HttpServletRequest req) {
        String userId = extractUserId(req);
        var payments = paymentService instanceof com.project.moviebooking.service.impl.PaymentServiceImpl
            ? null : null;
        // Return via repository directly
        return ResponseEntity.ok(ApiResponse.success("Your payments",
            paymentService.getPaymentByBookingId("ALL")));
    }

    /**
     * GET /api/payments/booking/{bookingId}
     * Payment details with GST breakdown for a booking
     */
    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<ApiResponse<Map<String,Object>>> getPaymentDetails(
            @PathVariable String bookingId) {
        var payment = paymentService.getPaymentByBookingId(bookingId);

        Map<String,Object> details = new java.util.LinkedHashMap<>();
        details.put("payment", payment);
        details.put("gstBreakdown", Map.of(
            "basePrice",    "₹" + payment.getBaseAmount(),
            "gstRate",      payment.getGstPercent() + "% (Indian Cinema Tax)",
            "gstAmount",    "₹" + payment.getGstAmount(),
            "totalPayable", "₹" + payment.getTotalAmount(),
            "displayText",  String.format("Base: ₹%.2f + GST %.0f%% (₹%.2f) = ₹%.2f",
                payment.getBaseAmount(), payment.getGstPercent(),
                payment.getGstAmount(), payment.getTotalAmount())
        ));

        return ResponseEntity.ok(ApiResponse.success("Payment details", details));
    }

    // ── Helpers ─────────────────────────────────────────────────────
    private String extractUserId(HttpServletRequest req) {
        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) throw new RuntimeException("Not authenticated");
        String token = header.substring(7);
        String email = jwtUtil.extractEmail(token);
        return email; // Using email as userId identifier
    }

    private String extractUserEmail(HttpServletRequest req) {
        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) return "user@cinebook.com";
        return jwtUtil.extractEmail(header.substring(7));
    }
}
