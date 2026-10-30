package com.toll.module4.pricing;

import com.toll.common.TripType;
import com.toll.common.VehicleType;
import com.toll.module1.plaza.IPlazaService;
import com.toll.module4.pricing.model.MonthlyPass;
import com.toll.module4.pricing.repository.IPassRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementation of IPricingService for Module 4.
 */
public class PricingServiceImpl implements IPricingService {
    private final IPassRepository passRepository;

    public PricingServiceImpl(IPassRepository passRepository) {
        if (passRepository == null) throw new IllegalArgumentException("Pass repository cannot be null.");
        this.passRepository = passRepository;
    }

    @Override
    public MonthlyPass issueMonthlyPass(String passId, String vehicleRegNo, int plazaId,
                                        LocalDate startDate, LocalDate endDate, double fee) {
        MonthlyPass pass = new MonthlyPass(passId, vehicleRegNo, plazaId, startDate, endDate, fee);
        passRepository.save(pass);
        return pass;
    }

    @Override
    public boolean hasActivePass(String vehicleRegNo, int plazaId, LocalDate date) {
        return passRepository.findActivePass(vehicleRegNo, plazaId, date).isPresent();
    }

    @Override
    public List<MonthlyPass> getAllPasses() {
        return passRepository.findAll();
    }

    @Override
    public List<MonthlyPass> getPassesForVehicle(String vehicleRegNo) {
        return passRepository.findAll().stream()
                .filter(p -> p.getVehicleRegistrationNumber().equalsIgnoreCase(vehicleRegNo.trim()))
                .toList();
    }

    /**
     * Mathematical rounding to the nearest Rs. 5.
     * Example: 72.5 -> 75, 72.4 -> 70, 77.5 -> 80
     */
    @Override
    public double roundToNearestFive(double amount) {
        return Math.round(amount / 5.0) * 5.0;
    }

    @Override
    public PricingResult calculateFare(int plazaId, String vehicleRegNo, VehicleType registeredType,
                                       VehicleType observedType, LocalDateTime crossingTime,
                                       boolean hadSingleTripWithin24h, IPlazaService plazaService) {
        LocalDate date = crossingTime.toLocalDate();

        // 1. Monthly Pass Check (Pays Rs. 0)
        if (hasActivePass(vehicleRegNo, plazaId, date)) {
            return new PricingResult(0.0, 0.0, TripType.PASS, false, true, false,
                    "Zero toll applied: Active Monthly Pass honored for this plaza.");
        }

        // 2. Base Fare Lookup for Registered Type
        double baseSingleRate = plazaService.getActiveRate(plazaId, registeredType, TripType.SINGLE, date);
        double appliedRate = baseSingleRate;
        TripType tripType = TripType.SINGLE;
        boolean returnDiscount = false;

        // 3. 24-Hour Return Trip Discount Check (50% of Single rate)
        if (hadSingleTripWithin24h) {
            appliedRate = baseSingleRate * 0.50;
            tripType = TripType.RETURN;
            returnDiscount = true;
        }

        // 4. Optical AVC Mismatch Check -> Charge HIGHER Toll
        boolean higherTollCharged = false;
        if (observedType != null && observedType != registeredType) {
            double observedSingleRate = plazaService.getActiveRate(plazaId, observedType, TripType.SINGLE, date);
            double observedRate = returnDiscount ? (observedSingleRate * 0.50) : observedSingleRate;

            if (observedRate > appliedRate) {
                appliedRate = observedRate;
                higherTollCharged = true;
            }
        }

        // 5. Round to nearest Rs. 5
        double finalRoundedFare = roundToNearestFive(appliedRate);

        String notes = String.format("Calculated fare: Rs. %.2f -> Rounded: Rs. %.2f (%s%s%s)",
                appliedRate, finalRoundedFare, tripType,
                returnDiscount ? ", 50% 24h Return Discount" : "",
                higherTollCharged ? ", Higher Toll Charged due to Type Mismatch" : "");

        return new PricingResult(baseSingleRate, finalRoundedFare, tripType, returnDiscount, false, higherTollCharged, notes);
    }
}
