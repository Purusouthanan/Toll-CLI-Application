package com.toll.module5.billing.model;

import java.time.LocalDateTime;

/**
 * Record representing a cash toll payment collected at a booth.
 * Captures amount, date, and the lane operator who collected it.
 */
public record CashPaymentRecord(
        String receiptId,
        String transactionId,
        int plazaId,
        int laneNumber,
        double amount,
        String operatorName,
        LocalDateTime collectedAt
) {}
