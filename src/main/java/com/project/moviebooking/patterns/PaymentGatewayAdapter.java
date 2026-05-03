package com.project.moviebooking.patterns;

import org.springframework.stereotype.Component;

/**
 * PaymentGatewayAdapter — Adapter Pattern (Structural)
 * ======================================================
 * PURPOSE:
 *   The ExternalPaymentAPI has an incompatible interface.
 *   We CANNOT change it (it's a 3rd party library).
 *   This Adapter wraps it and translates calls to our PaymentGateway interface.
 *
 * PATTERN: Adapter (Object Adapter variant)
 * PACKAGE: com.project.moviebooking.patterns
 *
 * ┌─────────────────┐    ┌──────────────────────────┐    ┌─────────────────────┐
 * │  PaymentService │───▶│  PaymentGatewayAdapter   │───▶│  ExternalPaymentAPI │
 * │ (Client)        │    │  implements PaymentGateway│    │  (Adaptee)          │
 * │                 │    │  wraps   ExternalPaymentAPI│   │  incompatible API   │
 * └─────────────────┘    └──────────────────────────┘    └─────────────────────┘
 *
 * SOLID — O: If we switch from Razorpay to Stripe, we create a new Adapter.
 *            PaymentService code doesn't change at all.
 * SOLID — D: PaymentService depends on PaymentGateway (interface, not this class)
 * ======================================================
 */
@Component
public class PaymentGatewayAdapter implements PaymentGateway {

    // ADAPTEE: the incompatible external library
    private final ExternalPaymentAPI externalAPI;

    // Fixed merchant ID for our CineBook system
    private static final String MERCHANT_ID = "CINEBOOK_MERCHANT_001";

    public PaymentGatewayAdapter() {
        // In production: inject via @Autowired or SDK initialization
        this.externalAPI = new ExternalPaymentAPI();
        System.out.println("🔌 [ADAPTER] PaymentGatewayAdapter initialized — wrapping ExternalPaymentAPI");
    }

    /**
     * ADAPTER: translates our processPayment() call
     * → ExternalPaymentAPI.initiateTransaction()
     *
     * Our interface:  processPayment(userId, amount, method)
     * External API:   initiateTransaction(merchantId, customerId, rupees, mode)
     */
    @Override
    public String processPayment(String userId, double amount, String method) {
        System.out.println("🔄 [ADAPTER] Translating processPayment → initiateTransaction");

        // ── TRANSLATION: our params → external API params ──
        String merchantId = MERCHANT_ID;
        String customerId = "CUST_" + userId;
        String externalMode = translatePaymentMode(method);  // translate naming

        // ── CALL external incompatible API ──
        String externalResponse = externalAPI.initiateTransaction(
                merchantId, customerId, amount, externalMode);

        // ── TRANSLATE response back to our format ──
        if (externalResponse != null && externalResponse.startsWith("TXN_SUCCESS_")) {
            String txnId = externalResponse.replace("TXN_SUCCESS_", "");
            System.out.println("✅ [ADAPTER] Payment success. TxnID: " + txnId);
            return txnId;
        }

        System.out.println("❌ [ADAPTER] Payment failed. Response: " + externalResponse);
        return null;
    }

    /**
     * ADAPTER: translates our refundPayment() call
     * → ExternalPaymentAPI.reverseTransaction()
     */
    @Override
    public boolean refundPayment(String transactionId) {
        System.out.println("🔄 [ADAPTER] Translating refundPayment → reverseTransaction");
        return externalAPI.reverseTransaction(transactionId);
    }

    /**
     * ADAPTER: translates our getPaymentStatus() call
     * → ExternalPaymentAPI.fetchTransactionStatus()
     */
    @Override
    public String getPaymentStatus(String transactionId) {
        System.out.println("🔄 [ADAPTER] Translating getPaymentStatus → fetchTransactionStatus");
        String externalStatus = externalAPI.fetchTransactionStatus(transactionId);

        // Translate external status to our enum-like strings
        return switch (externalStatus) {
            case "COMPLETED" -> "SUCCESS";
            case "PENDING"   -> "PENDING";
            default          -> "FAILED";
        };
    }

    /**
     * TRANSLATION helper: maps our payment method names to external API's naming
     * External API uses: "NET_BANKING", "CREDIT", "EWALLET"
     * Our system uses:   "UPI", "CARD", "WALLET"
     */
    private String translatePaymentMode(String ourMethod) {
        return switch (ourMethod.toUpperCase()) {
            case "UPI"                          -> "NET_BANKING";
            case "CREDIT_CARD", "DEBIT_CARD",
                 "CARD"                         -> "CREDIT";
            case "WALLET"                       -> "EWALLET";
            default                             -> "NET_BANKING";
        };
    }
}
