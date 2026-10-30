package com.toll.module5.billing.payment;

import com.toll.common.PaymentType;

/**
 * Result DTO returned by polymorphic PaymentMethod implementations.
 */
public record PaymentResult(
        boolean successful,
        PaymentType paymentType,
        double amountPaid,
        String referenceNumber,
        String message,
        boolean autoBlocked,
        boolean lowBalanceAlert
) {
    public static PaymentResult fastagSuccess(double amount, String tagId, double balance, boolean autoBlocked, boolean lowBal) {
        return new PaymentResult(true, PaymentType.FASTAG, amount, tagId,
                String.format("FASTag payment of Rs. %.2f approved. Remaining: Rs. %.2f", amount, balance),
                autoBlocked, lowBal);
    }

    public static PaymentResult cashSuccess(double amount, String operatorName, String receiptId) {
        return new PaymentResult(true, PaymentType.CASH, amount, receiptId,
                String.format("Cash payment of Rs. %.2f collected by Operator: %s (Receipt #%s)", amount, operatorName, receiptId),
                false, false);
    }

    public static PaymentResult exemptSuccess(String reason) {
        return new PaymentResult(true, PaymentType.EXEMPT, 0.0, "EXEMPT",
                "Toll exempt: " + reason, false, false);
    }

    public static PaymentResult failure(PaymentType type, double amount, String reason) {
        return new PaymentResult(false, type, amount, null, reason, false, false);
    }
}
