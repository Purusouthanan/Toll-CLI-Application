package com.toll.module4.pricing;

import com.toll.common.VehicleType;
import com.toll.module1.plaza.IPlazaService;
import com.toll.module4.pricing.model.MonthlyPass;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Interface contract for Module 4 (Rates, Discounts and Passes).
 * Rules:
 * 1. 24-Hour Return trip: 50% discount on Single trip rate.
 * 2. Monthly Pass: Rs. 0 toll at designated plaza.
 * 3. Only one active pass per vehicle per plaza at a time.
 * 4. Round to nearest Rs. 5.
 * 5. Optical AVC mismatch: Charge higher toll.
 */
public interface IPricingService {
    MonthlyPass issueMonthlyPass(String passId, String vehicleRegNo, int plazaId, LocalDate startDate, LocalDate endDate, double fee);
    boolean hasActivePass(String vehicleRegNo, int plazaId, LocalDate date);
    List<MonthlyPass> getAllPasses();
    List<MonthlyPass> getPassesForVehicle(String vehicleRegNo);

    double roundToNearestFive(double amount);

    PricingResult calculateFare(int plazaId, String vehicleRegNo, VehicleType registeredType,
                                VehicleType observedType, LocalDateTime crossingTime,
                                boolean hadSingleTripWithin24h, IPlazaService plazaService);
}
