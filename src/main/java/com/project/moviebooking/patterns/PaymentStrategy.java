package com.project.moviebooking.patterns;

/**
 * ===================================================
 * SOLID — O: Open/Closed Principle
 * PAYMENT STRATEGY PATTERN
 * ===================================================
 * Purpose: Define a family of payment algorithms,
 *          encapsulate each one, and make them interchangeable.
 *
 * Open for EXTENSION  → add new payment type (e.g., EMI) as new class
 * Closed for MODIFICATION → existing UPI/Card/Wallet code untouched
 *
 * This also demonstrates the STRATEGY behavioural pattern.
 * ===================================================
 */
public interface PaymentStrategy {

    /**
     * Execute the payment
     * @param userId   user making payment
     * @param amount   amount in INR
     * @param details  payment detail string (UPI ID / card number / wallet ID)
     * @return transaction ID if success, null if failed
     */
    String pay(String userId, double amount, String details);

    /**
     * Human-readable name of this payment strategy
     */
    String getMethodName();

    /**
     * Validate input before attempting payment
     */
    boolean validate(String details);
}
