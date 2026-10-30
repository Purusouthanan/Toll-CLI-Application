package com.toll.module3.tollengine;

import com.toll.common.VehicleType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable audit record capturing the details of a toll crossing transaction.
 * Demonstrates strong Encapsulation.
 */
public final class TollTransaction {
    private final String transactionId;
    private final String vehicleRegNo;
    private final int plazaId;
    private final int laneNumber;
    private final String tagId;
    private final LocalDateTime timestamp;
    private final double tollAmount;
    private final VehicleType observedType;
    private final VehicleType billedType;
    private final TripType tripType;
    private final PaymentMethod paymentMethod;
    private final String status;
    private final String failureReason;
    private final boolean isFlaggedForReview;
    private final String operatorName;

    private TollTransaction(Builder builder) {
        this.transactionId = builder.transactionId != null ? builder.transactionId : UUID.randomUUID().toString();
        this.vehicleRegNo = Objects.requireNonNull(builder.vehicleRegNo, "Vehicle registration cannot be null.");
        this.plazaId = builder.plazaId;
        this.laneNumber = builder.laneNumber;
        this.tagId = builder.tagId;
        this.timestamp = builder.timestamp != null ? builder.timestamp : LocalDateTime.now();
        this.tollAmount = builder.tollAmount;
        this.observedType = builder.observedType;
        this.billedType = builder.billedType;
        this.tripType = builder.tripType != null ? builder.tripType : TripType.SINGLE;
        this.paymentMethod = builder.paymentMethod != null ? builder.paymentMethod : PaymentMethod.FASTAG;
        this.status = builder.status != null ? builder.status : "PENDING";
        this.failureReason = builder.failureReason;
        this.isFlaggedForReview = builder.isFlaggedForReview;
        this.operatorName = builder.operatorName;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getVehicleRegNo() {
        return vehicleRegNo;
    }

    public int getPlazaId() {
        return plazaId;
    }

    public int getLaneNumber() {
        return laneNumber;
    }

    public String getTagId() {
        return tagId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public double getTollAmount() {
        return tollAmount;
    }

    public VehicleType getObservedType() {
        return observedType;
    }

    public VehicleType getBilledType() {
        return billedType;
    }

    public TripType getTripType() {
        return tripType;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public String getStatus() {
        return status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public boolean isFlaggedForReview() {
        return isFlaggedForReview;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public boolean isSuccess() {
        return "SUCCESS".equalsIgnoreCase(status);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String transactionId;
        private String vehicleRegNo;
        private int plazaId;
        private int laneNumber;
        private String tagId;
        private LocalDateTime timestamp;
        private double tollAmount;
        private VehicleType observedType;
        private VehicleType billedType;
        private TripType tripType;
        private PaymentMethod paymentMethod;
        private String status;
        private String failureReason;
        private boolean isFlaggedForReview;
        private String operatorName;

        public Builder transactionId(String transactionId) {
            this.transactionId = transactionId;
            return this;
        }

        public Builder vehicleRegNo(String vehicleRegNo) {
            this.vehicleRegNo = vehicleRegNo;
            return this;
        }

        public Builder plazaId(int plazaId) {
            this.plazaId = plazaId;
            return this;
        }

        public Builder laneNumber(int laneNumber) {
            this.laneNumber = laneNumber;
            return this;
        }

        public Builder tagId(String tagId) {
            this.tagId = tagId;
            return this;
        }

        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder tollAmount(double tollAmount) {
            this.tollAmount = tollAmount;
            return this;
        }

        public Builder observedType(VehicleType observedType) {
            this.observedType = observedType;
            return this;
        }

        public Builder billedType(VehicleType billedType) {
            this.billedType = billedType;
            return this;
        }

        public Builder tripType(TripType tripType) {
            this.tripType = tripType;
            return this;
        }

        public Builder paymentMethod(PaymentMethod paymentMethod) {
            this.paymentMethod = paymentMethod;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder failureReason(String failureReason) {
            this.failureReason = failureReason;
            return this;
        }

        public Builder flaggedForReview(boolean isFlaggedForReview) {
            this.isFlaggedForReview = isFlaggedForReview;
            return this;
        }

        public Builder operatorName(String operatorName) {
            this.operatorName = operatorName;
            return this;
        }

        public TollTransaction build() {
            return new TollTransaction(this);
        }
    }

    @Override
    public String toString() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return String.format("Txn[%s | Veh: %s | Plaza: %d/Lane %d | Amount: Rs. %.2f | Mode: %s | Status: %s | Flagged: %s | Time: %s%s]",
                transactionId.length() > 8 ? transactionId.substring(0, 8) : transactionId,
                vehicleRegNo, plazaId, laneNumber, tollAmount, paymentMethod, status,
                isFlaggedForReview ? "YES(AVC-Mismatch)" : "NO",
                timestamp.format(fmt),
                failureReason != null ? " | Reason: " + failureReason : "");
    }
}
