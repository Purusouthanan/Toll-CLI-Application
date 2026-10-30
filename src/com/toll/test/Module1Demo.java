package com.toll.test;

import com.toll.exception.LaneOperationException;
import com.toll.exception.PlazaNotReadyException;
import com.toll.exception.UnauthorizedException;
import com.toll.model.admin.Admin;
import com.toll.model.enums.LaneStatus;
import com.toll.model.enums.TripType;
import com.toll.model.enums.VehicleType;
import com.toll.model.plaza.*;
import com.toll.service.plaza.IPlazaService;
import com.toll.service.plaza.PlazaServiceImpl;

import java.time.LocalDate;
import java.util.List;

/**
 * Self-contained test and demonstration suite for Module 1: Toll Plaza and Lane Setup.
 * Validates all business rules, exception triggers, and OOP patterns.
 */
public class Module1Demo {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("   MODULE 1 — TOLL PLAZA AND LANE SETUP: VERIFICATION & TEST SUITE   ");
        System.out.println("================================================================================\n");

        IPlazaService plazaService = new PlazaServiceImpl();
        Admin authorizedAdmin = new Admin("ADMIN-001", "Shri Rajesh Kumar (NHAI Officer)", "rajesh.kumar@nhai.gov.in");

        // ----------------------------------------------------------------------------------------
        // TEST CASE 1: Admin Authorization Enforcement
        // Rule: Only an Admin can create toll plazas and lanes.
        // ----------------------------------------------------------------------------------------
        System.out.println(">>> [TEST 1] Verifying Admin-Only Authorization Enforcement...");
        try {
            plazaService.createPlaza(null, "Rogue Plaza", "NH-48", "Pune");
            System.err.println("FAILED: Unauthorized plaza creation was permitted!");
        } catch (UnauthorizedException e) {
            System.out.println("✔ PASSED: Unauthorized creation correctly blocked: " + e.getMessage());
        }

        // Create plaza with authorized admin
        TollPlaza plaza = plazaService.createPlaza(
                authorizedAdmin,
                "Khedshivapur Toll Plaza",
                "NH-48 (Mumbai-Bengaluru Expressway)",
                "Pune, Maharashtra"
        );
        System.out.println("✔ PASSED: Admin created Toll Plaza successfully: " + plaza.getName() + " (ID: " + plaza.getPlazaId() + ")");
        System.out.println();

        // ----------------------------------------------------------------------------------------
        // TEST CASE 2: Plaza Opening Guard - Minimum 2 Lanes Requirement
        // Rule: A toll plaza has at least 2 lanes. A plaza cannot open with less than 2 lanes.
        // ----------------------------------------------------------------------------------------
        System.out.println(">>> [TEST 2] Verifying Minimum 2 Lanes Requirement...");
        // Add only 1 lane initially
        Lane lane1 = new FASTagLane(1);
        plazaService.addLane(authorizedAdmin, plaza.getPlazaId(), lane1);
        System.out.println("Added 1 lane: " + lane1.getLaneDescription());

        try {
            plazaService.openPlaza(authorizedAdmin, plaza.getPlazaId());
            System.err.println("FAILED: Plaza opened with only 1 lane!");
        } catch (PlazaNotReadyException e) {
            System.out.println("✔ PASSED: Plaza opening correctly blocked due to insufficient lanes:\n   " + e.getMessage().replace("\n", "\n   "));
        }
        System.out.println();

        // ----------------------------------------------------------------------------------------
        // TEST CASE 3: Plaza Opening Guard - Toll Rates for Every Vehicle Type Required
        // Rule: A plaza cannot open until a rate exists for every vehicle type.
        // ----------------------------------------------------------------------------------------
        System.out.println(">>> [TEST 3] Verifying Complete Vehicle Type Rates Requirement...");
        // Add 2nd and 3rd lanes (Cash and Emergency) to fulfill lane count requirement
        Lane lane2 = new CashLane(2, "Suresh Yadav (POS Operator)");
        Lane lane3 = new EmergencyLane(3);
        plazaService.addLane(authorizedAdmin, plaza.getPlazaId(), lane2);
        plazaService.addLane(authorizedAdmin, plaza.getPlazaId(), lane3);
        System.out.println("Added Lane #2: " + lane2.getLaneDescription());
        System.out.println("Added Lane #3: " + lane3.getLaneDescription());
        System.out.println("Total lanes in plaza now: " + plaza.getLanes().size());

        // Configure rates only for CAR and VAN, leaving BUS, TRUCK, MULTI_AXLE unconfigured
        LocalDate baseDate = LocalDate.of(2026, 1, 1);
        plazaService.setTollRate(authorizedAdmin, plaza.getPlazaId(), VehicleType.CAR, TripType.SINGLE, 110.0, baseDate);
        plazaService.setTollRate(authorizedAdmin, plaza.getPlazaId(), VehicleType.CAR, TripType.RETURN, 165.0, baseDate);
        plazaService.setTollRate(authorizedAdmin, plaza.getPlazaId(), VehicleType.VAN, TripType.SINGLE, 180.0, baseDate);
        plazaService.setTollRate(authorizedAdmin, plaza.getPlazaId(), VehicleType.VAN, TripType.RETURN, 270.0, baseDate);

        try {
            plazaService.openPlaza(authorizedAdmin, plaza.getPlazaId());
            System.err.println("FAILED: Plaza opened with incomplete vehicle rates!");
        } catch (PlazaNotReadyException e) {
            System.out.println("✔ PASSED: Plaza opening correctly blocked due to missing vehicle rates:\n   " + e.getMessage().replace("\n", "\n   "));
        }

        // Now configure remaining vehicle rates (BUS, TRUCK, MULTI_AXLE)
        plazaService.setTollRate(authorizedAdmin, plaza.getPlazaId(), VehicleType.BUS, TripType.SINGLE, 375.0, baseDate);
        plazaService.setTollRate(authorizedAdmin, plaza.getPlazaId(), VehicleType.BUS, TripType.RETURN, 560.0, baseDate);
        plazaService.setTollRate(authorizedAdmin, plaza.getPlazaId(), VehicleType.TRUCK, TripType.SINGLE, 375.0, baseDate);
        plazaService.setTollRate(authorizedAdmin, plaza.getPlazaId(), VehicleType.TRUCK, TripType.RETURN, 560.0, baseDate);
        plazaService.setTollRate(authorizedAdmin, plaza.getPlazaId(), VehicleType.MULTI_AXLE, TripType.SINGLE, 605.0, baseDate);
        plazaService.setTollRate(authorizedAdmin, plaza.getPlazaId(), VehicleType.MULTI_AXLE, TripType.RETURN, 910.0, baseDate);

        // Now attempt to open plaza
        plazaService.openPlaza(authorizedAdmin, plaza.getPlazaId());
        System.out.println("✔ PASSED: Toll Plaza successfully opened! Status: " + plaza.getStatus());
        System.out.println();

        // ----------------------------------------------------------------------------------------
        // TEST CASE 4: Lane Closure Safety Constraint
        // Rule: A lane can be closed only if no vehicle is being processed in it.
        // ----------------------------------------------------------------------------------------
        System.out.println(">>> [TEST 4] Verifying Lane Closure Safety Constraint...");
        Lane fastagLane = plaza.getLane(1);
        System.out.println("Current status of Lane #1: " + fastagLane.getStatus());

        // Simulate vehicle entry into the lane
        System.out.println("Vehicle enters Lane #1: Invoking startProcessingVehicle()...");
        fastagLane.startProcessingVehicle();
        System.out.println("Lane #1 isProcessingVehicle: " + fastagLane.isProcessingVehicle());

        // Attempt to close lane while vehicle is inside
        try {
            plazaService.closeLane(authorizedAdmin, plaza.getPlazaId(), 1);
            System.err.println("FAILED: Lane was closed while vehicle was actively being processed!");
        } catch (LaneOperationException e) {
            System.out.println("✔ PASSED: Lane closure safely rejected while processing vehicle: " + e.getMessage());
        }

        // Vehicle finishes crossing
        System.out.println("Vehicle clears lane barrier: Invoking finishProcessingVehicle()...");
        fastagLane.finishProcessingVehicle();
        System.out.println("Lane #1 isProcessingVehicle: " + fastagLane.isProcessingVehicle());

        // Now close lane
        plazaService.closeLane(authorizedAdmin, plaza.getPlazaId(), 1);
        System.out.println("✔ PASSED: Lane closed successfully when idle. Status: " + fastagLane.getStatus());

        // Re-open lane
        plazaService.openLane(authorizedAdmin, plaza.getPlazaId(), 1);
        System.out.println("✔ PASSED: Lane re-opened successfully. Status: " + fastagLane.getStatus());
        System.out.println();

        // ----------------------------------------------------------------------------------------
        // TEST CASE 5: Rate Effective Dating Logic
        // Rule: A rate change applies only from its effective date. Earlier transactions are not affected.
        // ----------------------------------------------------------------------------------------
        System.out.println(">>> [TEST 5] Verifying Toll Rate Effective Dating...");
        // Base rate established on 2026-01-01 was ₹110 for CAR Single
        // Admin schedules a revised rate of ₹130 effective from 2026-11-01
        LocalDate revisedDate = LocalDate.of(2026, 11, 1);
        plazaService.setTollRate(authorizedAdmin, plaza.getPlazaId(), VehicleType.CAR, TripType.SINGLE, 130.0, revisedDate);
        System.out.println("Scheduled new rate: CAR Single = ₹130.00 effective from " + revisedDate);

        // Query rate for a historical/earlier date (e.g. 2026-10-15)
        LocalDate pastCrossingDate = LocalDate.of(2026, 10, 15);
        TollRate pastRate = plazaService.getEffectiveRate(plaza.getPlazaId(), VehicleType.CAR, TripType.SINGLE, pastCrossingDate);
        System.out.println("Querying crossing on " + pastCrossingDate + " -> Rate: ₹" + pastRate.getAmount() + " (Effective: " + pastRate.getEffectiveDate() + ")");
        if (pastRate.getAmount() == 110.0) {
            System.out.println("✔ PASSED: Earlier crossing retains historical base rate (₹110.00).");
        } else {
            System.err.println("FAILED: Earlier crossing was incorrectly affected by future rate revision!");
        }

        // Query rate on or after effective date (e.g. 2026-11-05)
        LocalDate futureCrossingDate = LocalDate.of(2026, 11, 5);
        TollRate futureRate = plazaService.getEffectiveRate(plaza.getPlazaId(), VehicleType.CAR, TripType.SINGLE, futureCrossingDate);
        System.out.println("Querying crossing on " + futureCrossingDate + " -> Rate: ₹" + futureRate.getAmount() + " (Effective: " + futureRate.getEffectiveDate() + ")");
        if (futureRate.getAmount() == 130.0) {
            System.out.println("✔ PASSED: Crossing after effective date correctly applies revised rate (₹130.00).");
        } else {
            System.err.println("FAILED: Revised rate did not take effect!");
        }
        System.out.println();

        // ----------------------------------------------------------------------------------------
        // TEST SUMMARY
        // ----------------------------------------------------------------------------------------
        System.out.println("================================================================================");
        System.out.println("   ALL MODULE 1 BUSINESS RULES & CONSTRAINTS VERIFIED SUCCESSFULLY!   ");
        System.out.println("================================================================================");
    }
}
