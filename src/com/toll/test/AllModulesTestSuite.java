package com.toll.test;

import com.toll.common.*;
import com.toll.module1.plaza.IPlazaService;
import com.toll.module1.plaza.PlazaServiceImpl;
import com.toll.module1.plaza.model.FASTagLane;
import com.toll.module1.plaza.model.TollPlaza;
import com.toll.module1.plaza.repository.InMemoryPlazaRepository;
import com.toll.module2.account.model.FASTagAccount;
import com.toll.module2.account.repository.InMemoryFASTagAccountRepository;
import com.toll.module2.account.repository.InMemoryVehicleRepository;
import com.toll.module2.account.service.AccountServiceImpl;
import com.toll.module2.account.service.IAccountService;
import com.toll.module3.tollengine.ITollProcessingService;
import com.toll.module3.tollengine.TollProcessingServiceImpl;
import com.toll.module3.tollengine.model.TollTransaction;
import com.toll.module3.tollengine.repository.InMemoryTransactionRepository;
import com.toll.module3.tollengine.repository.ITransactionRepository;
import com.toll.module4.pricing.IPricingService;
import com.toll.module4.pricing.PricingServiceImpl;
import com.toll.module4.pricing.model.MonthlyPass;
import com.toll.module4.pricing.repository.InMemoryPassRepository;
import com.toll.module5.billing.BillingReportingServiceImpl;
import com.toll.module5.billing.IBillingReportingService;
import com.toll.module5.billing.model.Dispute;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Master Automated Verification Suite covering ALL 5 Modules & End-to-End Business Workflows.
 */
public class AllModulesTestSuite {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("=========================================================================================");
        System.out.println("        COMPREHENSIVE END-TO-END VERIFICATION SUITE (ALL 5 MODULES INTEGRATED)           ");
        System.out.println("=========================================================================================");

        // Module 1 Tests
        runTest("M1-1: Plaza requires >= 2 lanes to open", AllModulesTestSuite::testPlazaMinimumTwoLanes);
        runTest("M1-2: Plaza cannot open without rates for ALL 5 vehicle types", AllModulesTestSuite::testPlazaRatesRequirement);
        runTest("M1-3: Lane close rejected when vehicle is being processed", AllModulesTestSuite::testLaneSafetyCloseRule);

        // Module 2 Tests
        runTest("M2-1: Unique vehicle registration and 1:1 tag link", AllModulesTestSuite::testModule2Rules);
        runTest("M2-2: Auto-block (< Rs. 100) and Auto-unblock (>= Rs. 100)", AllModulesTestSuite::testModule2StateTransitions);
        runTest("M2-3: Admin blacklisting refuses tag at toll plaza", AllModulesTestSuite::testBlacklistRefusal);

        // Module 3 & 4 Tests
        runTest("M3/4-1: Normal FASTag crossing opens barrier", AllModulesTestSuite::testNormalFASTagCrossing);
        runTest("M3/4-2: 24-hour return trip applies 50% discount automatically", AllModulesTestSuite::test24HourReturnDiscount);
        runTest("M3/4-3: Optical AVC mismatch charges higher toll and flags for review", AllModulesTestSuite::testAVCMismatchHigherToll);
        runTest("M3/4-4: Missing active tag diverted to Cash lane (Double Toll 2x)", AllModulesTestSuite::testMissingTagDoubleToll);
        runTest("M3/4-5: Insufficient balance diverted to Cash lane (Normal Toll)", AllModulesTestSuite::testInsufficientBalanceNormalCash);
        runTest("M3/4-6: Anti-double-billing cooldown prevents charge within 2 minutes", AllModulesTestSuite::testTwoMinuteCooldown);
        runTest("M3/4-7: Emergency vehicle in Emergency lane pays Rs. 0", AllModulesTestSuite::testEmergencyLaneBypass);
        runTest("M4-1: Active Monthly Pass allows Rs. 0 toll crossing", AllModulesTestSuite::testMonthlyPassCrossing);
        runTest("M4-2: Nearest Rs. 5 rounding algorithm", AllModulesTestSuite::testRoundingAlgorithm);

        // Module 5 Tests
        runTest("M5-1: 7-day dispute window (<=7 days allowed, >7 days rejected)", AllModulesTestSuite::testDisputeSevenDayWindow);
        runTest("M5-2: Approved dispute refunds money directly to FASTag balance", AllModulesTestSuite::testDisputeRefundToBalance);
        runTest("M5-3: Low balance alert triggers when balance < Rs. 200", AllModulesTestSuite::testLowBalanceAlert);
        runTest("M5-4: SQL Queries 1, 2, 3 in-memory aggregations", AllModulesTestSuite::testSQLQueriesAggregation);

        System.out.println("=========================================================================================");
        System.out.printf(" INTEGRATION TEST SUMMARY: Total: %d | Passed: %d | Failed: %d\n",
                (passed + failed), passed, failed);
        System.out.println("=========================================================================================");

        if (failed > 0) {
            System.err.println("SOME TESTS FAILED!");
            System.exit(1);
        } else {
            System.out.println("ALL INTEGRATION TESTS PASSED WITH 100% SUCCESS!");
        }
    }

    private static void runTest(String name, Runnable test) {
        try {
            test.run();
            System.out.printf(" [PASS] %s\n", name);
            passed++;
        } catch (Throwable t) {
            System.err.printf(" [FAIL] %s - %s\n", name, t.getMessage());
            t.printStackTrace(System.err);
            failed++;
        }
    }

    // --- Module 1 Tests ---
    private static void testPlazaMinimumTwoLanes() {
        InMemoryPlazaRepository repo = new InMemoryPlazaRepository();
        IPlazaService service = new PlazaServiceImpl(repo);
        service.createPlaza(101, "Test Plaza", "NH-1", "Delhi");
        service.addLane(101, 1, "FASTAG"); // Only 1 lane

        // Set rates for all 5 types
        for (VehicleType vt : VehicleType.values()) {
            service.setTollRate(101, vt, TripType.SINGLE, 100.0, LocalDate.now());
        }

        boolean opened = service.openPlaza(101);
        assertFalse(opened, "Plaza with only 1 lane must not be allowed to open");

        service.addLane(101, 2, "CASH"); // Now 2 lanes
        boolean openedWithTwo = service.openPlaza(101);
        assertTrue(openedWithTwo, "Plaza with 2 lanes and all rates must open");
    }

    private static void testPlazaRatesRequirement() {
        InMemoryPlazaRepository repo = new InMemoryPlazaRepository();
        IPlazaService service = new PlazaServiceImpl(repo);
        service.createPlaza(102, "Rate Test Plaza", "NH-2", "Agra");
        service.addLane(102, 1, "FASTAG");
        service.addLane(102, 2, "CASH");

        // Set rates for only 3 types (missing Bus and MultiAxle)
        service.setTollRate(102, VehicleType.CAR, TripType.SINGLE, 50.0, LocalDate.now());
        service.setTollRate(102, VehicleType.VAN, TripType.SINGLE, 75.0, LocalDate.now());
        service.setTollRate(102, VehicleType.TRUCK, TripType.SINGLE, 150.0, LocalDate.now());

        boolean opened = service.openPlaza(102);
        assertFalse(opened, "Plaza missing rates for all 5 vehicle types must not open");

        service.setTollRate(102, VehicleType.BUS, TripType.SINGLE, 120.0, LocalDate.now());
        service.setTollRate(102, VehicleType.MULTI_AXLE, TripType.SINGLE, 200.0, LocalDate.now());
        assertTrue(service.openPlaza(102), "Plaza with rates for all 5 vehicle types should open");
    }

    private static void testLaneSafetyCloseRule() {
        FASTagLane lane = new FASTagLane(1);
        assertTrue(lane.close(), "Idle lane should close safely");
        lane.open();

        lane.setProcessingVehicle(true); // Vehicle entering lane
        boolean closedWhileBusy = lane.close();
        assertFalse(closedWhileBusy, "Lane must NOT close while processing a vehicle!");

        lane.setProcessingVehicle(false); // Vehicle cleared
        assertTrue(lane.close(), "Lane should close after vehicle clears");
    }

    // --- Module 2 Tests ---
    private static void testModule2Rules() {
        AccountServiceImpl acc = new AccountServiceImpl(new InMemoryVehicleRepository(), new InMemoryFASTagAccountRepository());
        acc.registerVehicle(VehicleType.CAR, "DL-01-AA-1111", "Tester", "9876543210");
        acc.issueFASTag("DL-01-AA-1111", "TAG-1111", 500.0);

        try {
            acc.issueFASTag("DL-01-AA-1111", "TAG-2222", 500.0);
            throw new AssertionError("Should not link 2nd active tag to same vehicle");
        } catch (Exception expected) {}
    }

    private static void testModule2StateTransitions() {
        AccountServiceImpl acc = new AccountServiceImpl(new InMemoryVehicleRepository(), new InMemoryFASTagAccountRepository());
        acc.registerVehicle(VehicleType.CAR, "DL-01-BB-2222", "Tester 2", "9876543210");
        acc.issueFASTag("DL-01-BB-2222", "TAG-STATE", 140.0);

        acc.deductToll("TAG-STATE", 50.0); // Balance = 90 -> Auto-block
        FASTagAccount a = acc.findAccountByTagId("TAG-STATE").orElseThrow();
        assertEquals(AccountStatus.BLOCKED, a.getStatus(), "Should be BLOCKED");

        acc.rechargeAccount("TAG-STATE", 100.0); // Balance = 190 -> Auto-unblock
        assertEquals(AccountStatus.ACTIVE, a.getStatus(), "Should be ACTIVE");
    }

    private static void testBlacklistRefusal() {
        Context ctx = createContext();
        ctx.accountService.blacklistTag("IND-TAG-1001", "Suspicious activity");

        var resp = ctx.tollService.processFASTagCrossing(1, 1, "IND-TAG-1001", VehicleType.CAR, LocalDateTime.now());
        assertFalse(resp.barrierOpened(), "Barrier must not open for blacklisted tag");
        assertTrue(resp.message().contains("BLACKLISTED"), "Message must mention blacklisting");
    }

    // --- Module 3 & 4 Tests ---
    private static void testNormalFASTagCrossing() {
        Context ctx = createContext();
        var resp = ctx.tollService.processFASTagCrossing(2, 1, "IND-TAG-1001", VehicleType.CAR, LocalDateTime.now());
        assertTrue(resp.barrierOpened(), "Barrier should open on successful deduction");
        assertEquals(80.0, resp.transaction().tollAmount(), 0.001, "Toll amount should be 80.00");
    }

    private static void test24HourReturnDiscount() {
        Context ctx = createContext();
        // Car DL-03-TC-9988 had single trip 5 hours ago at Plaza 1
        // Now returns at Plaza 1: Single rate is 165.00 -> 50% = 82.50 -> rounded to nearest 5 = 85.00!
        var resp = ctx.tollService.processFASTagCrossing(1, 1, "IND-TAG-1003", VehicleType.VAN, LocalDateTime.now());
        assertTrue(resp.barrierOpened(), "Barrier should open");
        assertEquals(85.0, resp.transaction().tollAmount(), 0.001, "Return rate should be 85.00");
        assertEquals(TripType.RETURN, resp.transaction().tripType(), "Trip type must be RETURN");
    }

    private static void testAVCMismatchHigherToll() {
        Context ctx = createContext();
        // Tag IND-TAG-1001 is registered as CAR (Single rate at Plaza 2 = 80.00)
        // Optical AVC observes TRUCK (Single rate at Plaza 2 = 260.00)
        // Rule: Higher toll (260.00) charged and flagged for review
        var resp = ctx.tollService.processFASTagCrossing(2, 1, "IND-TAG-1001", VehicleType.TRUCK, LocalDateTime.now());
        assertTrue(resp.barrierOpened(), "Barrier should open");
        assertEquals(260.0, resp.transaction().tollAmount(), 0.001, "Higher truck toll must be charged");
        assertTrue(resp.transaction().flaggedForReview(), "Transaction must be flagged for review");
    }

    private static void testMissingTagDoubleToll() {
        Context ctx = createContext();
        // Non-existent tag in FASTag lane
        var resp = ctx.tollService.processFASTagCrossing(1, 1, "TAG-NONEXISTENT", VehicleType.CAR, LocalDateTime.now());
        assertFalse(resp.barrierOpened(), "Barrier stays closed");
        assertTrue(resp.redirectedToCash(), "Vehicle should be redirected to cash lane");
        assertTrue(resp.isDoubleTollPenalty(), "Should be marked for double toll penalty (2x)");
    }

    private static void testInsufficientBalanceNormalCash() {
        Context ctx = createContext();
        // IND-TAG-1002 has balance Rs. 85.00 (Blocked)
        var resp = ctx.tollService.processFASTagCrossing(1, 1, "IND-TAG-1002", VehicleType.CAR, LocalDateTime.now());
        assertFalse(resp.barrierOpened(), "Barrier stays closed");
        assertTrue(resp.redirectedToCash(), "Diverted to cash lane");
        assertFalse(resp.isDoubleTollPenalty(), "Normal toll applies in cash lane (not double)");
    }

    private static void testTwoMinuteCooldown() {
        Context ctx = createContext();
        LocalDateTime now = LocalDateTime.now();
        // First crossing
        var resp1 = ctx.tollService.processFASTagCrossing(2, 1, "IND-TAG-1001", VehicleType.CAR, now);
        assertTrue(resp1.barrierOpened(), "First crossing should succeed");

        // Second crossing 30 seconds later at same plaza
        var resp2 = ctx.tollService.processFASTagCrossing(2, 1, "IND-TAG-1001", VehicleType.CAR, now.plusSeconds(30));
        assertFalse(resp2.barrierOpened(), "Second crossing within 2 minutes must be rejected");
        assertTrue(resp2.message().contains("2 minutes"), "Cooldown message check");
    }

    private static void testEmergencyLaneBypass() {
        Context ctx = createContext();
        var resp = ctx.tollService.processEmergencyCrossing(1, 4, "AMB-MH-12-999", "Ambulance", LocalDateTime.now());
        assertTrue(resp.barrierOpened(), "Emergency vehicle barrier must open");
        assertEquals(0.0, resp.transaction().tollAmount(), 0.001, "Toll must be Rs. 0");
    }

    private static void testMonthlyPassCrossing() {
        Context ctx = createContext();
        // MH-12-DE-1433 has active monthly pass at Plaza 1
        var resp = ctx.tollService.processFASTagCrossing(1, 1, "IND-TAG-1001", VehicleType.CAR, LocalDateTime.now());
        assertTrue(resp.barrierOpened(), "Barrier opens");
        assertEquals(0.0, resp.transaction().tollAmount(), 0.001, "Monthly pass toll must be 0");
        assertEquals(TripType.PASS, resp.transaction().tripType(), "Trip type must be PASS");
    }

    private static void testRoundingAlgorithm() {
        IPricingService ps = new PricingServiceImpl(new InMemoryPassRepository());
        assertEquals(70.0, ps.roundToNearestFive(72.4), 0.001, "72.4 -> 70");
        assertEquals(75.0, ps.roundToNearestFive(72.5), 0.001, "72.5 -> 75");
        assertEquals(105.0, ps.roundToNearestFive(102.6), 0.001, "102.6 -> 105");
        assertEquals(100.0, ps.roundToNearestFive(101.0), 0.001, "101.0 -> 100");
    }

    // --- Module 5 Tests ---
    private static void testDisputeSevenDayWindow() {
        Context ctx = createContext();
        LocalDateTime sixDaysAgo = LocalDateTime.now().minusDays(6);
        LocalDateTime eightDaysAgo = LocalDateTime.now().minusDays(8);

        // Within 7 days -> Allowed
        Dispute d = ctx.billingService.raiseDispute("TXN-1", "TAG-1", "REG-1", 100.0, sixDaysAgo, "Wrong charge");
        assertEquals(DisputeStatus.RAISED, d.getStatus(), "Dispute status should be RAISED");

        // Older than 7 days -> Rejected
        try {
            ctx.billingService.raiseDispute("TXN-2", "TAG-1", "REG-1", 100.0, eightDaysAgo, "Too late");
            throw new AssertionError("Dispute older than 7 days must be rejected!");
        } catch (IllegalArgumentException expected) {}
    }

    private static void testDisputeRefundToBalance() {
        Context ctx = createContext();
        double initialBal = ctx.accountService.findAccountByTagId("IND-TAG-1001").orElseThrow().getBalance();

        Dispute d = ctx.billingService.raiseDispute("TXN-REFUND", "IND-TAG-1001", "MH-12-DE-1433", 100.0,
                LocalDateTime.now().minusDays(2), "Duplicate deduction refund");

        ctx.billingService.approveDispute(d.getDisputeId(), "Approved by Plaza Admin");

        double newBal = ctx.accountService.findAccountByTagId("IND-TAG-1001").orElseThrow().getBalance();
        assertEquals(initialBal + 100.0, newBal, 0.001, "Refund must be credited to tag balance");
    }

    private static void testLowBalanceAlert() {
        Context ctx = createContext();
        // Register vehicle with Rs. 250 balance
        ctx.accountService.registerVehicle(VehicleType.CAR, "MP-09-ALERT-1", "Alert Owner", "9876543210");
        ctx.accountService.issueFASTag("MP-09-ALERT-1", "TAG-ALERT-100", 250.00);

        // Plaza 2 Car toll is Rs. 80 -> remaining balance = 250 - 80 = 170 (< Rs. 200 threshold)
        var resp = ctx.tollService.processFASTagCrossing(2, 1, "TAG-ALERT-100", VehicleType.CAR, LocalDateTime.now());
        assertTrue(resp.barrierOpened(), "Barrier opened");

        boolean alertGenerated = ctx.billingService.getAllAlerts().stream()
                .anyMatch(a -> "TAG-ALERT-100".equals(a.tagId()));
        assertTrue(alertGenerated, "Low balance alert must be generated when balance < Rs. 200");
    }

    private static void testSQLQueriesAggregation() {
        Context ctx = createContext();
        // Query 1: Vehicle type summary (>2 successful txns)
        var q1 = ctx.billingService.getQuery1VehicleTypeSummary();
        assertFalse(q1.isEmpty(), "Query 1 should return results");

        // Query 2: Plaza performance (>=3 crossings)
        var q2 = ctx.billingService.getQuery2PlazaPerformance();
        assertFalse(q2.isEmpty(), "Query 2 should return results");

        // Query 3: Multi-plaza accounts (>2 distinct plazas)
        var q3 = ctx.billingService.getQuery3MultiPlazaAccounts();
        assertFalse(q3.isEmpty(), "Query 3 should return accounts that crossed >2 plazas");
    }

    // --- Helpers ---
    private static class Context {
        IPlazaService plazaService;
        IAccountService accountService;
        IPricingService pricingService;
        IBillingReportingService billingService;
        ITransactionRepository transactionRepository;
        ITollProcessingService tollService;
    }

    private static Context createContext() {
        Context ctx = new Context();
        var plazaRepo = new InMemoryPlazaRepository();
        ctx.plazaService = new PlazaServiceImpl(plazaRepo);

        ctx.accountService = new AccountServiceImpl(
                new InMemoryVehicleRepository(),
                new InMemoryFASTagAccountRepository()
        );

        var passRepo = new InMemoryPassRepository();
        ctx.pricingService = new PricingServiceImpl(passRepo);

        ctx.transactionRepository = new InMemoryTransactionRepository();

        ctx.billingService = new BillingReportingServiceImpl(
                ctx.accountService,
                ctx.transactionRepository,
                plazaRepo
        );

        ctx.tollService = new TollProcessingServiceImpl(
                ctx.plazaService,
                ctx.accountService,
                ctx.pricingService,
                ctx.billingService,
                ctx.transactionRepository
        );

        DataSeeder.seedAllModules(ctx.plazaService, ctx.accountService, ctx.pricingService, ctx.billingService, ctx.transactionRepository);
        return ctx;
    }

    private static void assertTrue(boolean c, String m) { if (!c) throw new AssertionError("FAIL: " + m); }
    private static void assertFalse(boolean c, String m) { if (c) throw new AssertionError("FAIL: " + m); }
    private static void assertEquals(Object e, Object a, String m) {
        if (!e.equals(a)) throw new AssertionError("FAIL: " + m + ". Expected: " + e + ", Actual: " + a);
    }
    private static void assertEquals(double e, double a, double d, String m) {
        if (Math.abs(e - a) > d) throw new AssertionError(String.format("FAIL: %s. Expected: %.2f, Actual: %.2f", m, e, a));
    }
}
