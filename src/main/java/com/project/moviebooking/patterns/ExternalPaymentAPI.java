package com.project.moviebooking.patterns;

/**
 * ExternalPaymentAPI — Simulates an incompatible 3rd party payment SDK
 * ======================================================================
 * This class represents an EXTERNAL library/API that we do NOT control.
 * Its method names and signatures are incompatible with our system.
 *
 * In production this would be: Razorpay SDK, Stripe SDK, etc.
 * We CANNOT modify this class (it's a 3rd party dependency).
 *
 * The Adapter Pattern solves this by wrapping it.
 * ======================================================================
 */
public class ExternalPaymentAPI {

    /**
     * Incompatible method — uses different naming convention
     * Returns: "TXN_SUCCESS_<id>" or "TXN_FAILED_<reason>"
     */
    public String initiateTransaction(String merchantId,
                                      String customerId,
                                      double rupees,
                                      String mode) {
        System.out.println("💼 [EXTERNAL API] initiateTransaction called");
        System.out.println("   Merchant: " + merchantId +
                           " | Customer: " + customerId +
                           " | Amount: ₹" + rupees +
                           " | Mode: " + mode);

        // Simulate external API — always succeeds in demo
        String txnId = "EXT_" + System.currentTimeMillis();
        return "TXN_SUCCESS_" + txnId;
    }

    /**
     * Incompatible method — different naming for refunds
     */
    public boolean reverseTransaction(String externalTxnId) {
        System.out.println("↩️ [EXTERNAL API] reverseTransaction: " + externalTxnId);
        return true;
    }

    /**
     * Incompatible method — different naming for status check
     */
    public String fetchTransactionStatus(String externalTxnId) {
        return "COMPLETED";
    }
}
