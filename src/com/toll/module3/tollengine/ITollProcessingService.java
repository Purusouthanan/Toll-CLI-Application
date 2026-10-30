package com.toll.module3.tollengine;

import com.toll.common.VehicleType;

/**
 * Interface contract for Member 3: Module 3 (Toll Processing Engine).
 * Responsibilities:
 * - Scans tag, opens barrier only after toll deducted successfully.
 * - AVC mismatch: Charges higher toll and flags transaction for review.
 * - Vehicle without valid active tag: Sent to cash lane, pays DOUBLE toll (2x).
 * - Insufficient tag balance: Sent to cash lane, pays NORMAL toll in cash.
 * - Anti-tailgating / Cooldown: Same tag cannot be charged twice at same plaza within 2 minutes.
 * - Emergency vehicles pay nothing in Emergency lane.
 */
public interface ITollProcessingService {
    CrossingResult processCrossing(int plazaId, int laneNumber, String tagId, VehicleType observedType);
    CrossingResult processEmergencyCrossing(int plazaId, int laneNumber, String vehicleRegNo);
    CrossingResult processCashCrossing(int plazaId, int laneNumber, String vehicleRegNo, VehicleType vehicleType, String operatorName, boolean isDoubleTollPenalty);

    record CrossingResult(boolean barrierOpened, String transactionId, double chargedAmount, String status, String message, boolean flaggedForReview) {}
}
