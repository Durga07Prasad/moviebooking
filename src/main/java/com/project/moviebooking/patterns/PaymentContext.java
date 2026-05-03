package com.project.moviebooking.patterns;

import org.springframework.stereotype.Component;
import java.util.List;

/**
 * PaymentContext — Strategy Pattern Selector
 * ===========================================
 * PATTERN: Strategy (Behavioral)
 * SOLID: O (Open/Closed) — add new strategy = new class, no changes here
 * SOLID: D — depends on PaymentStrategy interface list
 *
 * Also integrates the ADAPTER pattern:
 * Each strategy (UPI/Card/Wallet) internally delegates to
 * PaymentGatewayAdapter which adapts ExternalPaymentAPI.
 *
 * Flow:
 * PaymentServiceImpl → PaymentContext.executePayment()
 *                    → [selects Strategy: UPI/Card/Wallet]
 *                    → Strategy.pay()
 *                    → PaymentGatewayAdapter.processPayment()
 *                    → ExternalPaymentAPI.initiateTransaction()
 * ===========================================
 */
@Component
public class PaymentContext {

    private final List<PaymentStrategy>    strategies;
    private final PaymentGatewayAdapter    adapter; // ADAPTER injected here

    // SOLID: D — Spring injects ALL PaymentStrategy beans automatically
    public PaymentContext(List<PaymentStrategy> strategies,
                          PaymentGatewayAdapter adapter) {
        this.strategies = strategies;
        this.adapter    = adapter;
        System.out.println("✅ [STRATEGY] PaymentContext loaded "
                + strategies.size() + " strategies:");
        strategies.forEach(s -> System.out.println("   → " + s.getMethodName()));
    }

    /**
     * setStrategy(String) — alias so callers can set+execute separately.
     * Kept for backward compatibility with code expecting:
     *   paymentContext.setStrategy("UPI");
     *   paymentContext.executePayment(userId, amount, details);
     *
     * Internally resolves and caches the strategy for the next executePayment call.
     */
    private PaymentStrategy cachedStrategy;

    public void setStrategy(String method) {
        this.cachedStrategy = resolveStrategy(method);
        System.out.println("[STRATEGY] setStrategy called with: " + method
                + " → " + cachedStrategy.getMethodName());
    }

    /**
     * Select strategy at runtime and execute payment.
     * Preferred API: executePayment(method, userId, amount, details)
     */
    public String executePayment(String method, String userId,
                                 double amount, String details) {

        PaymentStrategy strategy = resolveStrategy(method);
        System.out.println("\n[STRATEGY] Selected: " + strategy.getMethodName());

        String safeDetails = (details != null && !details.isBlank())
                ? details : "demo@upi";

        try {
            String txnId = strategy.pay(userId, amount, safeDetails);
            if (txnId != null) {
                System.out.println("[ADAPTER] Verifying via PaymentGatewayAdapter...");
                adapter.processPayment(userId, amount, method);
            }
            return txnId;
        } catch (Exception e) {
            System.out.println("❌ [STRATEGY] Payment error: " + e.getMessage());
            return null;
        }
    }

    private PaymentStrategy resolveStrategy(String method) {
        if (method == null) return getDefault();
        String norm = method.toUpperCase().replace("_", "").replace(" ", "");
        return strategies.stream()
                .filter(s -> norm.contains(s.getMethodName().replace("_","")))
                .findFirst()
                .orElse(getDefault());
    }

    private PaymentStrategy getDefault() {
        return strategies.stream()
                .filter(s -> "UPI".equals(s.getMethodName()))
                .findFirst()
                .orElse(strategies.get(0));
    }
}
