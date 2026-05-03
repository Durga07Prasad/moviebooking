package com.project.moviebooking.patterns;

import org.springframework.stereotype.Component;
import java.util.UUID;

/**
 * UPI Payment Strategy
 * SOLID: O (Open/Closed) — NEW payment type added as NEW class, no existing code changed
 */
@Component
public class UPIPaymentStrategy implements PaymentStrategy {

    @Override
    public String pay(String userId, double amount, String upiId) {
        // Simulated UPI payment — in production: call Razorpay / PhonePe API
        System.out.println("📱 [UPI] Processing ₹" + amount + " for UPI ID: " + upiId);

        // Simulate network call (100ms delay)
        try { Thread.sleep(100); } catch (InterruptedException ignored) {}

        String txnId = "UPI_" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        System.out.println("✅ [UPI] Transaction ID: " + txnId);
        return txnId;
    }

    @Override
    public String getMethodName() { return "UPI"; }

    @Override
    public boolean validate(String details) {
        // UPI ID must contain @ symbol
        return details != null && details.contains("@");
    }
}
