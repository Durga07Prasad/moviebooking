package com.project.moviebooking.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO: Payment request from payment page
 */
@Data
public class PaymentRequest {

    @NotBlank(message = "Booking ID is required")
    private String bookingId;

    @NotBlank(message = "Payment method is required")
    private String paymentMethod;  // CREDIT_CARD, DEBIT_CARD, UPI, WALLET

    private String cardNumber;     // optional, simulated

    private String upiId;          // optional, simulated
}
