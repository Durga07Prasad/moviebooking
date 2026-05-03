package com.project.moviebooking.service;

import org.springframework.stereotype.Component;

/**
 * RefundService — Calculates booking cancellation refund eligibility.
 *
 * OOAD: GRASP Information Expert — this class owns all refund business rules.
 * SOLID: SRP — only one reason to change: if refund policy changes.
 * SOLID: O  — new refund tiers can be added without modifying callers.
 *
 * Policy (mirrors BookMyShow cancellation rules):
 *   Show already started       → No refund
 *   Within 2 hours of show     → No refund
 *   More than 2 hours before   → 50% refund of base price
 *   NOTE: GST is NEVER refunded per Indian tax regulation.
 */
@Component
public class RefundService {

    /**
     * RefundResult — Value Object (GRASP: Information Expert).
     *
     * Encapsulates the complete refund decision so that callers
     * receive a single coherent result rather than multiple values.
     * SOLID: SRP — purely a data carrier with no behaviour.
     */
    public static class RefundResult {
        public final boolean eligible;
        public final double  refundAmount;
        public final String  reason;
        public final String  refundStatus;  // PROCESSED / NOT_ELIGIBLE

        public RefundResult(boolean eligible, double refundAmount,
                            String reason, String refundStatus) {
            this.eligible     = eligible;
            this.refundAmount = refundAmount;
            this.reason       = reason;
            this.refundStatus = refundStatus;
        }

        @Override public String toString() {
            return "RefundResult{eligible=" + eligible +
                   ", amount=₹" + refundAmount +
                   ", status=" + refundStatus + "}";
        }
    }

    /**
     * Calculates refund eligibility based on IST show time.
     *
     * OOAD: This method is the Information Expert for cancellation policy.
     * It uses ISTTimeService (injected as parameter for testability — avoids
     * hard dependency, demonstrating SOLID DIP even without interface).
     *
     * @param basePricePaid  The base ticket price the user paid (EXCLUDING GST)
     * @param showDate       The show date string "YYYY-MM-DD"
     * @param showTime       The show time string "HH:mm"
     * @param ist            ISTTimeService for accurate IST comparison
     * @return RefundResult  Complete refund decision object
     */
    public RefundResult calculate(double basePricePaid,
                                  String showDate,
                                  String showTime,
                                  ISTTimeService ist) {

        long hours = ist.hoursUntilShow(showDate, showTime);

        // ── Rule 1: Show already started ──────────────────────────────
        if (hours < 0) {
            return new RefundResult(
                false,
                0.0,
                "Show has already started. No refund.",
                "NOT_ELIGIBLE"
            );
        }

        // ── Rule 2: Within 2 hours of show ────────────────────────────
        if (hours < 2) {
            long minutesLeft = ist.minutesUntilShow(showDate, showTime);
            return new RefundResult(
                false,
                0.0,
                "Cancelled within 2 hours of show (" + minutesLeft + " min left). No refund.",
                "NOT_ELIGIBLE"
            );
        }

        // ── Rule 3: More than 2 hours before show → 50% refund ────────
        double refund = Math.round(basePricePaid * 0.50 * 100.0) / 100.0;
        String displayTime = ist.formatTo12Hour(showTime);

        return new RefundResult(
            true,
            refund,
            String.format(
                "50%% refund of ₹%.2f = ₹%.2f approved. " +
                "GST is non-refundable per Indian tax law. " +
                "Show starts at %s in %dh — cancelled with sufficient notice.",
                basePricePaid, refund, displayTime, hours
            ),
            "PROCESSED"
        );
    }
}
