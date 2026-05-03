package com.project.moviebooking.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Payment — stores payment details with full GST breakdown.
 *
 * NOTE on status fields:
 *   paymentStatus — primary field ("SUCCESS", "FAILED", "REFUNDED", "PENDING")
 *   status        — alias kept for backward-compat with any code calling setStatus()
 * Both are kept in sync via the manual setStatus() / getStatus() methods.
 *
 * SOLID: O — new fields added without touching existing code
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "payments")
public class Payment {

    @Id
    private String id;

    private String bookingId;

    private String userId;

    private double baseAmount;

    private double gstPercent;

    private double gstAmount;

    private double totalAmount;

    private String paymentMethod;   // UPI / CARD / WALLET

    private String paymentStatus = "PENDING";  // SUCCESS / FAILED / REFUNDED

    /** Alias field — kept in sync with paymentStatus */
    private String status = "PENDING";

    private String transactionId;

    private LocalDateTime paymentTime = LocalDateTime.now();

    // Refund fields
    private double refundAmount = 0;

    private String refundStatus = "NONE";   // NONE / PROCESSED / NOT_ELIGIBLE

    private String refundReason = "";

    private LocalDateTime refundTime;

    /**
     * setStatus — sets BOTH paymentStatus and status alias simultaneously.
     * Fixes: "The method setStatus(String) is undefined for the type Payment"
     */
    public void setStatus(String s) {
        this.paymentStatus = s;
        this.status = s;
    }

    /**
     * getStatus — reads from paymentStatus (primary field).
     * Fixes: "The method getStatus() is undefined for the type Payment"
     */
    public String getStatus() {
        return this.paymentStatus;
    }
}

