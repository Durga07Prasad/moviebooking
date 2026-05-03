package com.project.moviebooking.patterns;

/**
 * PaymentGateway — Our internal payment interface
 * ================================================
 * Adapter Pattern: TARGET interface
 *
 * This is what our application code expects.
 * It is clean, domain-specific, and uses our naming conventions.
 * ================================================
 */
public interface PaymentGateway {

    /**
     * Process a payment
     * @param userId      our system user ID
     * @param amount      amount in INR
     * @param method      "UPI" | "CARD" | "WALLET"
     * @return transaction ID if success, null if failed
     */
    String processPayment(String userId, double amount, String method);

    /**
     * Refund a payment
     * @param transactionId the transaction to reverse
     * @return true if refund successful
     */
    boolean refundPayment(String transactionId);

    /**
     * Check status of a transaction
     * @return "SUCCESS" | "FAILED" | "PENDING"
     */
    String getPaymentStatus(String transactionId);
}
