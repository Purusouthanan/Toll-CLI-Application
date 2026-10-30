package com.toll.module5.billing.payment;

/**
 * Value object encapsulating all parameters needed to execute a polymorphic toll payment.
 */
public record PaymentContext(
        String tagId,
        String vehicleRegNo,
        int plazaId,
        int laneNumber,
        double amount,
        String operatorName
) {}
