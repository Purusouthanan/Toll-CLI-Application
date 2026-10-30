package com.toll.module3.tollengine.model;

import com.toll.common.PaymentType;
import com.toll.common.TransactionStatus;
import com.toll.common.TripType;
import com.toll.common.VehicleType;

import java.time.LocalDateTime;

/**
 * Immutable entity capturing a complete Toll Plaza Crossing Transaction.
 */
public record TollTransaction(
        String transactionId,
        String vehicleRegNo,
        int plazaId,
        int laneNumber,
        String tagId,
        LocalDateTime timestamp,
        double tollAmount,
        VehicleType observedType,
        VehicleType billedType,
        TripType tripType,
        PaymentType paymentMethod,
        TransactionStatus status,
        boolean flaggedForReview,
        String operatorName,
        String failureReason
) {
    public boolean isSuccessful() {
        return status == TransactionStatus.SUCCESS;
    }
}
