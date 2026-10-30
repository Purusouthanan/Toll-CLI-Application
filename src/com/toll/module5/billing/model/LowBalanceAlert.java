package com.toll.module5.billing.model;

import java.time.LocalDateTime;

/**
 * Record representing an automated Low Balance Alert.
 * Triggered automatically when FASTag balance drops below Rs. 200.00 after a toll deduction.
 */
public record LowBalanceAlert(
        String alertId,
        String tagId,
        String vehicleRegNo,
        double balanceAfterDeduction,
        LocalDateTime alertTime,
        String message
) {}
