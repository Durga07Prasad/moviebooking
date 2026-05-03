package com.project.moviebooking.service;

import com.project.moviebooking.model.Payment;
import java.util.Map;

/**
 * PaymentService — DIP interface for payment processing
 * SOLID: D — controllers depend on this interface, not PaymentServiceImpl
 */
public interface PaymentService {

    /** Process payment with GST calculation — returns {payment, gstBreakdown} */
    Map<String, Object> processPayment(String bookingId, String paymentMethod,
                                       String userId, String upiId, String cardNumber);

    /** Cancel booking with refund eligibility check */
    Map<String, Object> cancelAndRefund(String bookingId, String userId);

    /** Get payment details for a booking */
    Payment getPaymentByBookingId(String bookingId);
}
