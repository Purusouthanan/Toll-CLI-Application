package com.toll.module4.pricing;

import com.toll.common.TripType;

/**
 * Result DTO capturing the pricing evaluation breakdown.
 */
public record PricingResult(
        double baseFare,
        double roundedFinalFare,
        TripType tripType,
        boolean returnDiscountApplied,
        boolean passApplied,
        boolean higherTollCharged,
        String pricingNotes
) {}
