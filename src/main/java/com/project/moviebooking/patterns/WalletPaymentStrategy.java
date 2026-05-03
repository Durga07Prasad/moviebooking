package com.project.moviebooking.patterns;

import org.springframework.stereotype.Component;
import java.util.UUID;

/**
 * Wallet Payment Strategy
 * SOLID: O — Third payment type added without any modification to existing classes
 */
@Component
public class WalletPaymentStrategy implements PaymentStrategy {

    @Override
    public String pay(String userId, double amount, String walletId) {
        System.out.println("👜 [WALLET] Processing ₹" + amount + " from wallet: " + walletId);

        try { Thread.sleep(80); } catch (InterruptedException ignored) {}

        String txnId = "WLT_" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        System.out.println("✅ [WALLET] Transaction ID: " + txnId);
        return txnId;
    }

    @Override
    public String getMethodName() { return "WALLET"; }

    @Override
    public boolean validate(String details) {
        return details != null && !details.isBlank();
    }
}
