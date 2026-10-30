package com.toll.module3.tollengine;

import com.toll.common.VehicleType;
import com.toll.module3.tollengine.model.TollTransaction;

import java.time.LocalDateTime;

/**
 * Interface contract for Module 3 (Toll Processing Engine).
 */
public interface ITollProcessingService {

    /**
     * Processes crossing at a FASTag Lane.
     */
    CrossingResponse processFASTagCrossing(int plazaId, int laneNumber, String tagId,
                                          VehicleType observedType, LocalDateTime crossingTime);

    /**
     * Processes crossing at a Cash Lane.
     * Can be normal cash crossing, insufficient balance fallback (normal toll), or no-tag penalty (double toll).
     */
    CrossingResponse processCashCrossing(int plazaId, int laneNumber, String vehicleRegNo,
                                        VehicleType vehicleType, String operatorName,
                                        boolean isDoubleTollPenalty, LocalDateTime crossingTime);

    /**
     * Processes crossing at an Emergency Lane for Ambulance, Fire Engine, etc. (Rs. 0 toll).
     */
    CrossingResponse processEmergencyCrossing(int plazaId, int laneNumber, String vehicleRegNo,
                                             String emergencyType, LocalDateTime crossingTime);

    record CrossingResponse(
            boolean barrierOpened,
            TollTransaction transaction,
            String message,
            boolean redirectedToCash,
            boolean isDoubleTollPenalty
    ) {}
}
