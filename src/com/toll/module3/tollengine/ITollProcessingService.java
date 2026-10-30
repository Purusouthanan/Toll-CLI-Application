package com.toll.module3.tollengine;

import com.toll.common.VehicleType;

import java.time.LocalDateTime;
import java.util.List;

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

    /**
     * Processes a vehicle crossing through an Electronic Toll Collection (FASTag) lane.
     */
    CrossingResult processCrossing(int plazaId, int laneNumber, String tagId, VehicleType observedType);

    /**
     * Overloaded crossing processor accepting explicit crossing timestamp (for simulation/deterministic testing).
     */
    CrossingResult processCrossing(int plazaId, int laneNumber, String tagId, VehicleType observedType, LocalDateTime crossingTime);

    /**
     * Processes a vehicle crossing through an Emergency Lane.
     * Authorized emergency vehicles pay nothing (Rs. 0). Non-emergency vehicles are refused.
     */
    CrossingResult processEmergencyCrossing(int plazaId, int laneNumber, String vehicleRegNo);

    /**
     * Overloaded emergency crossing accepting explicit timestamp.
     */
    CrossingResult processEmergencyCrossing(int plazaId, int laneNumber, String vehicleRegNo, LocalDateTime crossingTime);

    /**
     * Processes a vehicle payment in a Cash Lane with operator logging.
     */
    CrossingResult processCashCrossing(int plazaId, int laneNumber, String vehicleRegNo, 
                                        VehicleType vehicleType, String operatorName, boolean isDoubleTollPenalty);

    /**
     * Overloaded cash crossing accepting explicit timestamp.
     */
    CrossingResult processCashCrossing(int plazaId, int laneNumber, String vehicleRegNo, 
                                        VehicleType vehicleType, String operatorName, boolean isDoubleTollPenalty, LocalDateTime crossingTime);

    /**
     * Settles a vehicle previously diverted from a FASTag lane in a designated cash lane.
     */
    CrossingResult processDivertedVehicleInCash(CrossingResult divertedResult, int cashLaneNumber, String operatorName);

    /**
     * Checks if the 2-minute anti-double-billing cooldown is active for this tag at this plaza.
     */
    boolean isCooldownActive(String tagId, int plazaId, LocalDateTime currentTime);

    /**
     * Returns remaining seconds in the 2-minute anti-tailgating cooldown window.
     */
    long getRemainingCooldownSeconds(String tagId, int plazaId, LocalDateTime currentTime);

    /**
     * Accesses the boom barrier actuator for a specific plaza and lane.
     */
    BarrierGate getBarrier(int plazaId, int laneNumber);

    /**
     * Returns an unmodifiable list of all recorded toll transactions.
     */
    List<TollTransaction> getAllTransactions();

    /**
     * Returns all transactions flagged for administrative audit (e.g. AVC vehicle type mismatch).
     */
    List<TollTransaction> getFlaggedTransactions();

    /**
     * Registers a single trip toll rate for a vehicle type at a plaza.
     */
    void registerBaseRate(int plazaId, VehicleType vehicleType, double singleRate);

    /**
     * Configures monthly pass status for a vehicle at a plaza.
     */
    void registerMonthlyPass(String vehicleRegNo, int plazaId, boolean active);

    /**
     * Sets lane operational status (Open/Closed).
     */
    void setLaneOpen(int plazaId, int laneNumber, boolean isOpen);

    /**
     * Immutable result DTO returned by the crossing pipeline.
     */
    record CrossingResult(boolean barrierOpened, 
                          String transactionId, 
                          double chargedAmount, 
                          String status, 
                          String message, 
                          boolean flaggedForReview) {
        public boolean isSuccess() {
            return "SUCCESS".equalsIgnoreCase(status);
        }
        public boolean isDivertedToCash() {
            return message != null && message.contains("Diverted to Cash Lane");
        }
        public boolean isDoubleTollPenalty() {
            return message != null && message.contains("Double Toll");
        }
    }
}
