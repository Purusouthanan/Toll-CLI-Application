package com.toll.module5.billing.model;

import com.toll.common.DisputeStatus;

import java.time.LocalDateTime;

/**
 * Entity representing a Dispute filed on a wrong toll deduction.
 * Rule: Must be filed within 7 days of the transaction timestamp.
 * Rule: If approved by Admin, the amount is automatically refunded to the tag balance.
 */
public class Dispute {
    private final String disputeId;
    private final String transactionId;
    private final String tagId;
    private final String vehicleRegNo;
    private final double disputedAmount;
    private final String reason;
    private final LocalDateTime raisedAt;
    private DisputeStatus status;
    private String adminRemarks;
    private LocalDateTime resolvedAt;

    public Dispute(String disputeId, String transactionId, String tagId, String vehicleRegNo,
                   double disputedAmount, String reason) {
        if (disputeId == null || disputeId.trim().isEmpty()) throw new IllegalArgumentException("Dispute ID cannot be empty.");
        if (transactionId == null || transactionId.trim().isEmpty()) throw new IllegalArgumentException("Transaction ID cannot be empty.");
        if (disputedAmount <= 0.0) throw new IllegalArgumentException("Disputed amount must be positive.");

        this.disputeId = disputeId.trim().toUpperCase();
        this.transactionId = transactionId.trim();
        this.tagId = tagId;
        this.vehicleRegNo = vehicleRegNo;
        this.disputedAmount = disputedAmount;
        this.reason = reason;
        this.raisedAt = LocalDateTime.now();
        this.status = DisputeStatus.RAISED;
    }

    public String getDisputeId() {
        return disputeId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getTagId() {
        return tagId;
    }

    public String getVehicleRegNo() {
        return vehicleRegNo;
    }

    public double getDisputedAmount() {
        return disputedAmount;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getRaisedAt() {
        return raisedAt;
    }

    public synchronized DisputeStatus getStatus() {
        return status;
    }

    public synchronized String getAdminRemarks() {
        return adminRemarks;
    }

    public synchronized LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public synchronized void approve(String remarks) {
        this.status = DisputeStatus.APPROVED;
        this.adminRemarks = remarks;
        this.resolvedAt = LocalDateTime.now();
    }

    public synchronized void reject(String remarks) {
        this.status = DisputeStatus.REJECTED;
        this.adminRemarks = remarks;
        this.resolvedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return String.format("Dispute[%s | Txn: %s | Tag: %s | Rs. %.2f | Status: %s | Reason: %s]",
                disputeId, transactionId, tagId, disputedAmount, status, reason);
    }
}
