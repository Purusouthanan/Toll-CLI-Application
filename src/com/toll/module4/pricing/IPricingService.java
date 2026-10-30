package com.toll.module4.pricing;

import com.toll.common.VehicleType;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Interface contract for Member 4: Module 4 (Rates, Discounts and Passes).
 * Responsibilities:
 * - 24-hour return trip discount: 50% of single trip rate applied automatically.
 * - Monthly pass per plaza: Pays Rs. 0 toll when active pass exists.
 * - Only one active monthly pass per vehicle per plaza.
 * - Toll rounding: All calculated toll amounts rounded to nearest Rs. 5.
 */
public interface IPricingService {
    double calculateToll(int plazaId, String vehicleRegNo, VehicleType vehicleType, LocalDateTime crossingTime);
    void issueMonthlyPass(String passId, String vehicleRegNo, int plazaId, LocalDate startDate, LocalDate endDate, double fee);
    boolean hasActivePass(String vehicleRegNo, int plazaId, LocalDate date);
    double roundToNearestFive(double amount);
}
