package com.toll.test;

import com.toll.common.DataSeeder;
import com.toll.common.VehicleType;
import com.toll.module2.account.repository.InMemoryFASTagAccountRepository;
import com.toll.module2.account.repository.InMemoryVehicleRepository;
import com.toll.module2.account.service.AccountServiceImpl;
import com.toll.module2.account.service.IAccountService;
import com.toll.module3.tollengine.ITollProcessingService;
import com.toll.module3.tollengine.ITollProcessingService.CrossingResult;
import com.toll.module3.tollengine.TollProcessingServiceImpl;

import java.time.LocalDateTime;

/**
 * Automated Verification Test Suite for Module 3: Toll Processing Engine.
 * Tests all statutory rules, edge cases, and 6 core acceptance criteria.
 */
public class Module3TestSuite {

    private static int totalTests = 0;
    private static int passedTests = 0;
    private static int failedTests = 0;

    public static void main(String[] args) {
        System.out.println("===============================================================================");
        System.out.println("        RUNNING MODULE 3 (TOLL PROCESSING ENGINE) TEST SUITE                  ");
        System.out.println("===============================================================================");

        testStandardFASTagCrossing();
        testEmergencyLaneBypass();
        testEmergencyLaneUnauthorizedRefusal();
        testAntiDoubleBillingTwoMinuteCooldown();
        testCooldownDifferentPlazaAllowed();
        testCooldownExpirationAllowed();
        testAVCMismatchChargesHigherTollAndFlags();
        testAVCMatchStandardCharge();
        testMissingTagDoubleTollPenalty();
        testInsufficientBalanceNormalTollDiversion();
        testCashLaneSettlementWithOperator();
        testTwentyFourHourReturnTripDiscount();
        testBlacklistedTagRefusal();
        testMonthlyPassTollExemption();
        testClosedLaneSafetyGuard();

        printSummary();

        if (failedTests > 0) {
            System.exit(1);
        }
    }

    private static void assertTrue(String testName, boolean condition, String details) {
        totalTests++;
        if (condition) {
            passedTests++;
            System.out.println(" [PASS] " + testName + (details != null ? " (" + details + ")" : ""));
        } else {
            failedTests++;
            System.err.println(" [FAIL] " + testName + " --> FAILED! " + (details != null ? details : ""));
        }
    }

    private static ITollProcessingService createTestEngine() {
        IAccountService accountService = new AccountServiceImpl(
                new InMemoryVehicleRepository(),
                new InMemoryFASTagAccountRepository()
        );
        DataSeeder.seedModule2Data(accountService);
        return new TollProcessingServiceImpl(accountService);
    }

    private static void testStandardFASTagCrossing() {
        ITollProcessingService service = createTestEngine();
        // IND-TAG-1001 is CAR (MH-12-DE-1433), initial balance Rs. 1500.00, Plaza 1 Car rate = Rs. 80.00
        CrossingResult result = service.processCrossing(1, 1, "IND-TAG-1001", VehicleType.CAR);

        assertTrue("1. Standard FASTag Crossing (Deduction & Barrier Open)",
                result.barrierOpened() && result.isSuccess() && result.chargedAmount() == 80.0 && !result.flaggedForReview(),
                "Charged: Rs. 80, Barrier: OPEN");
    }

    private static void testEmergencyLaneBypass() {
        ITollProcessingService service = createTestEngine();
        CrossingResult result = service.processEmergencyCrossing(1, 9, "MH-12-AMB-108");

        assertTrue("2. Emergency Vehicle Clearance (Ambulance Rs. 0)",
                result.barrierOpened() && result.isSuccess() && result.chargedAmount() == 0.0,
                "Toll: Rs. 0.00, Barrier: OPEN");
    }

    private static void testEmergencyLaneUnauthorizedRefusal() {
        ITollProcessingService service = createTestEngine();
        CrossingResult result = service.processEmergencyCrossing(1, 9, "MH-12-PVT-9999");

        assertTrue("3. Non-Emergency Vehicle in Emergency Lane Refused",
                !result.barrierOpened() && !result.isSuccess() && result.message().contains("unauthorized"),
                "Refused: Unauthorized vehicle");
    }

    private static void testAntiDoubleBillingTwoMinuteCooldown() {
        ITollProcessingService service = createTestEngine();
        LocalDateTime t0 = LocalDateTime.of(2026, 10, 30, 10, 0, 0);

        // 1st crossing at 10:00:00 -> SUCCESS
        CrossingResult res1 = service.processCrossing(1, 1, "IND-TAG-1001", VehicleType.CAR, t0);

        // 2nd crossing at 10:00:30 (30 seconds later) -> REJECTED
        LocalDateTime t1 = t0.plusSeconds(30);
        CrossingResult resDup = service.processCrossing(1, 1, "IND-TAG-1001", VehicleType.CAR, t1);

        assertTrue("4. 2-Minute Anti-Double-Billing Cooldown Active",
                res1.isSuccess() && !resDup.isSuccess() && !resDup.barrierOpened() &&
                resDup.message().contains("Duplicate crossing rejected"),
                "Rejected after 30s at same plaza");
    }

    private static void testCooldownDifferentPlazaAllowed() {
        ITollProcessingService service = createTestEngine();
        LocalDateTime t0 = LocalDateTime.of(2026, 10, 30, 10, 0, 0);

        service.processCrossing(1, 1, "IND-TAG-1001", VehicleType.CAR, t0);
        // Crossing at Plaza 2 after 30s -> ALLOWED (Cooldown is scoped per-plaza)
        CrossingResult resPlaza2 = service.processCrossing(2, 1, "IND-TAG-1001", VehicleType.CAR, t0.plusSeconds(30));

        assertTrue("5. Crossing at Different Plaza Not Blocked by Cooldown",
                resPlaza2.isSuccess() && resPlaza2.barrierOpened(),
                "Allowed at Plaza #2");
    }

    private static void testCooldownExpirationAllowed() {
        ITollProcessingService service = createTestEngine();
        LocalDateTime t0 = LocalDateTime.of(2026, 10, 30, 10, 0, 0);

        service.processCrossing(1, 1, "IND-TAG-1001", VehicleType.CAR, t0);
        // Crossing after 125 seconds (> 2 minutes) -> ALLOWED
        CrossingResult resAfter2Min = service.processCrossing(1, 1, "IND-TAG-1001", VehicleType.CAR, t0.plusSeconds(125));

        assertTrue("6. Crossing Allowed After 2-Minute Cooldown Expires",
                resAfter2Min.isSuccess() && resAfter2Min.barrierOpened(),
                "Allowed after 125s");
    }

    private static void testAVCMismatchChargesHigherTollAndFlags() {
        ITollProcessingService service = createTestEngine();
        // IND-TAG-1001 is registered as CAR (Rate Rs. 80). AVC Camera observes TRUCK (Rate Rs. 270)
        CrossingResult result = service.processCrossing(1, 1, "IND-TAG-1001", VehicleType.TRUCK);

        assertTrue("7. AVC Mismatch Charges Higher Toll and Flags for Review",
                result.isSuccess() && result.flaggedForReview() && result.chargedAmount() == 270.0,
                "Charged Truck rate Rs. 270 instead of Rs. 80, Flagged = TRUE");
    }

    private static void testAVCMatchStandardCharge() {
        ITollProcessingService service = createTestEngine();
        // IND-TAG-1004 is registered as BUS (Rate Rs. 270). AVC Camera observes BUS.
        CrossingResult result = service.processCrossing(1, 1, "IND-TAG-1004", VehicleType.BUS);

        assertTrue("8. AVC Match Charges Standard Toll Without Review Flag",
                result.isSuccess() && !result.flaggedForReview() && result.chargedAmount() == 270.0,
                "Charged Rs. 270, Flagged = FALSE");
    }

    private static void testMissingTagDoubleTollPenalty() {
        ITollProcessingService service = createTestEngine();
        // Vehicle in FASTag lane without a tag
        CrossingResult result = service.processCrossing(1, 1, null, VehicleType.CAR);

        assertTrue("9. Vehicle Without Tag Diverted with Double Toll (2x Penalty)",
                !result.barrierOpened() && result.isDivertedToCash() && result.isDoubleTollPenalty() &&
                result.chargedAmount() == 160.0,
                "Diverted: YES, Penalty Due: Rs. 160.00 (2x Rs. 80)");
    }

    private static void testInsufficientBalanceNormalTollDiversion() {
        ITollProcessingService service = createTestEngine();
        // IND-TAG-1002 has balance Rs. 80.00 (Auto-blocked < Rs. 100). Car rate = Rs. 80.00
        CrossingResult result = service.processCrossing(1, 1, "IND-TAG-1002", VehicleType.CAR);

        assertTrue("10. Insufficient Balance Diverted with Normal Toll (Not Double)",
                !result.barrierOpened() && result.isDivertedToCash() && !result.isDoubleTollPenalty() &&
                result.chargedAmount() == 80.0,
                "Diverted: YES, Due: NORMAL Rs. 80.00");
    }

    private static void testCashLaneSettlementWithOperator() {
        ITollProcessingService service = createTestEngine();
        // 1st step: divert vehicle without tag
        CrossingResult diverted = service.processCrossing(1, 1, null, VehicleType.CAR);

        // 2nd step: settle in cash lane #2 with operator Suresh
        CrossingResult settled = service.processDivertedVehicleInCash(diverted, 2, "Operator-Suresh");

        assertTrue("11. Diverted Vehicle Settles in Cash Lane with Operator Name",
                settled.barrierOpened() && settled.isSuccess() && settled.chargedAmount() == 160.0,
                "Collected: Rs. 160 by Operator-Suresh");
    }

    private static void testTwentyFourHourReturnTripDiscount() {
        ITollProcessingService service = createTestEngine();
        LocalDateTime t1 = LocalDateTime.of(2026, 10, 30, 8, 0, 0);

        // Trip 1 (Single) at 8:00 AM -> Rs. 80.00
        CrossingResult trip1 = service.processCrossing(1, 1, "IND-TAG-1001", VehicleType.CAR, t1);

        // Trip 2 (Return) at 2:00 PM (6 hours later, within 24h) -> 50% = Rs. 40.00
        LocalDateTime t2 = t1.plusHours(6);
        CrossingResult trip2 = service.processCrossing(1, 1, "IND-TAG-1001", VehicleType.CAR, t2);

        assertTrue("12. 24-Hour Return Trip Automatically Gets 50% Discount",
                trip1.chargedAmount() == 80.0 && trip2.chargedAmount() == 40.0 && trip2.isSuccess(),
                "Trip 1: Rs. 80, Return Trip: Rs. 40 (50% off)");
    }

    private static void testBlacklistedTagRefusal() {
        IAccountService accountService = new AccountServiceImpl(
                new InMemoryVehicleRepository(),
                new InMemoryFASTagAccountRepository()
        );
        DataSeeder.seedModule2Data(accountService);
        accountService.blacklistTag("IND-TAG-1001", "Reported stolen");

        ITollProcessingService service = new TollProcessingServiceImpl(accountService);
        CrossingResult result = service.processCrossing(1, 1, "IND-TAG-1001", VehicleType.CAR);

        assertTrue("13. Blacklisted Tag Refused at Plaza with Alarm",
                !result.barrierOpened() && !result.isSuccess() && result.message().contains("BLACKLISTED"),
                "Refused: Tag Blacklisted");
    }

    private static void testMonthlyPassTollExemption() {
        ITollProcessingService service = createTestEngine();
        // Register monthly pass for vehicle MH-12-DE-1433 at Plaza 1
        service.registerMonthlyPass("MH-12-DE-1433", 1, true);

        CrossingResult result = service.processCrossing(1, 1, "IND-TAG-1001", VehicleType.CAR);

        assertTrue("14. Monthly Pass Holder Charged Rs. 0 Toll",
                result.barrierOpened() && result.isSuccess() && result.chargedAmount() == 0.0,
                "Toll: Rs. 0.00, Barrier: OPEN");
    }

    private static void testClosedLaneSafetyGuard() {
        ITollProcessingService service = createTestEngine();
        service.setLaneOpen(1, 3, false); // Close lane 3

        CrossingResult result = service.processCrossing(1, 3, "IND-TAG-1001", VehicleType.CAR);

        assertTrue("15. Closed Lane Safety Guard Blocks Vehicle Entry",
                !result.barrierOpened() && !result.isSuccess() && result.message().contains("CLOSED"),
                "Blocked: Lane CLOSED");
    }

    private static void printSummary() {
        System.out.println("===============================================================================");
        System.out.printf(" TEST SUMMARY: Total: %d | Passed: %d | Failed: %d%n", totalTests, passedTests, failedTests);
        System.out.println("===============================================================================");
        if (failedTests == 0) {
            System.out.println("ALL MODULE 3 TESTS PASSED PERFECTLY!");
        } else {
            System.out.println("SOME TESTS FAILED! PLEASE REVIEW OUTPUT ABOVE.");
        }
    }
}
