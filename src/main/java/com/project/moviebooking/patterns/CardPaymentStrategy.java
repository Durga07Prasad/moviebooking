package com.project.moviebooking.patterns;

import org.springframework.stereotype.Component;
import java.util.UUID;

/**
 * Card Payment Strategy (Credit + Debit)
 * SOLID: O — Added without touching UPIPaymentStrategy or PaymentStrategy interface
 */
@Component
public class CardPaymentStrategy implements PaymentStrategy {

    @Override
    public String pay(String userId, double amount, String cardNumber) {
        System.out.println("💳 [CARD] Processing ₹" + amount + " for card: ****" +
                (cardNumber != null && cardNumber.length() >= 4
                        ? cardNumber.substring(cardNumber.length() - 4) : "XXXX"));

        try { Thread.sleep(150); } catch (InterruptedException ignored) {}

        String txnId = "CARD_" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        System.out.println("✅ [CARD] Transaction ID: " + txnId);
        return txnId;
    }

    @Override
    public String getMethodName() { return "CARD"; }

    @Override
    public boolean validate(String details) {
        // Card number must be 12-19 digits
        return details != null && details.replaceAll("\\s","").matches("\\d{12,19}");
    }
}
