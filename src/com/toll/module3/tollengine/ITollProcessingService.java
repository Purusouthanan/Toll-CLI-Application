package com.toll.module3.tollengine;

import com.toll.common.VehicleType;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Interface contract for Module 3 (Toll Processing Engine).
 * Fully unifies standalone engine capabilities and 5-module integrated crossing pipelines.
 */
public interface ITollProcessingService {

    // --- Standalone & Automated Suite Crossing Methods ---
    CrossingResult processCrossing(int plazaId, int laneNumber, String tagId, VehicleType observedType);
    CrossingResult processCrossing(int plazaId, int laneNumber, String tagId, VehicleType observedType, LocalDateTime crossingTime);

    CrossingResult processEmergencyCrossing(int plazaId, int laneNumber, String vehicleRegNo);
    CrossingResult processEmergencyCrossing(int plazaId, int laneNumber, String vehicleRegNo, LocalDateTime crossingTime);
    CrossingResult processEmergencyCrossing(int plazaId, int laneNumber, String vehicleRegNo, String emergencyType, LocalDateTime crossingTime);

    CrossingResult processCashCrossing(int plazaId, int laneNumber, String vehicleRegNo, 
                                      VehicleType vehicleType, String operatorName, boolean isDoubleTollPenalty);
    CrossingResult processCashCrossing(int plazaId, int laneNumber, String vehicleRegNo, 
                                      VehicleType vehicleType, String operatorName, boolean isDoubleTollPenalty, LocalDateTime crossingTime);

    CrossingResult processDivertedVehicleInCash(CrossingResult divertedResult, int cashLaneNumber, String operatorName);

    boolean isCooldownActive(String tagId, int plazaId, LocalDateTime currentTime);
    long getRemainingCooldownSeconds(String tagId, int plazaId, LocalDateTime currentTime);
    BarrierGate getBarrier(int plazaId, int laneNumber);

    List<TollTransaction> getAllTransactions();
    List<TollTransaction> getFlaggedTransactions();

    void registerBaseRate(int plazaId, VehicleType vehicleType, double singleRate);
    void registerMonthlyPass(String vehicleRegNo, int plazaId, boolean active);
    void setLaneOpen(int plazaId, int laneNumber, boolean isOpen);

    // --- 5-Module Integrated Pipeline Method ---
    CrossingResponse processFASTagCrossing(int plazaId, int laneNumber, String tagId,
                                          VehicleType observedType, LocalDateTime crossingTime);

    // --- Result DTOs ---
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

    record CrossingResponse(
            boolean barrierOpened,
            com.toll.module3.tollengine.model.TollTransaction transaction,
            String message,
            boolean redirectedToCash,
            boolean isDoubleTollPenalty
    ) {}
}
