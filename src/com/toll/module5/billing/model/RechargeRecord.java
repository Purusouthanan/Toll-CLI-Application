package com.toll.module5.billing.model;

import com.toll.common.RechargeMethod;
import com.toll.common.RechargeStatus;

import java.time.LocalDateTime;

/**
 * Record representing a FASTag account recharge transaction.
 * Stores amount, date, method (UPI, Card, Net Banking), and status (Paid, Failed, Refunded).
 */
public record RechargeRecord(
        String rechargeId,
        String tagId,
        double amount,
        RechargeMethod method,
        RechargeStatus status,
        LocalDateTime timestamp,
        String failureReason
) {}
