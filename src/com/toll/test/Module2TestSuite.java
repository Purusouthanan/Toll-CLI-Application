package com.toll.test;

import com.toll.common.AccountStatus;
import com.toll.common.VehicleType;
import com.toll.module2.account.exception.*;
import com.toll.module2.account.model.*;
import com.toll.module2.account.repository.InMemoryFASTagAccountRepository;
import com.toll.module2.account.repository.InMemoryVehicleRepository;
import com.toll.module2.account.service.*;

/**
 * Comprehensive Automated Verification Suite for Module 2 (Vehicle & FASTag Accounts).
 * Validates all OOP tenets and NHAI business rules without requiring external testing frameworks.
 */
public class Module2TestSuite {
    private static int testsPassed = 0;
    private static int testsFailed = 0;

    public static void main(String[] args) {
        System.out.println("===============================================================================");
        System.out.println("        RUNNING MODULE 2 (VEHICLE & FASTAG ACCOUNTS) TEST SUITE                ");
        System.out.println("===============================================================================");

        runTest("1. Unique Vehicle Registration Rule", Module2TestSuite::testUniqueVehicleRegistration);
        runTest("2. Strict 1:1 Tag-to-Vehicle Association", Module2TestSuite::testOneToOneTagAssociation);
        runTest("3. Auto-Block State Machine (Balance < Rs. 100)", Module2TestSuite::testAutoBlockWhenBalanceBelow100);
        runTest("4. Minimum Recharge Limit (Min Rs. 100)", Module2TestSuite::testRechargeMinimumLimit);
        runTest("5. Maximum Recharge Limit (Max Rs. 10,000)", Module2TestSuite::testRechargeMaximumLimit);
        runTest("6. Maximum Balance Cap (Max Rs. 50,000)", Module2TestSuite::testMaxBalanceLimit50000);
        runTest("7. Auto-Unblock State Machine (Recharge to >= Rs. 100)", Module2TestSuite::testAutoUnblockOnRecharge);
        runTest("8. Non-Negative Balance Constraint", Module2TestSuite::testNonNegativeBalanceConstraint);
        runTest("9. Admin Blacklist & Plaza Refusal", Module2TestSuite::testAdminBlacklistRefusal);
        runTest("10. Dispute Refund Credit & State Unblock", Module2TestSuite::testDisputeRefundCredit);
        runTest("11. OOP Inheritance & Polymorphism (Vehicle Hierarchy)", Module2TestSuite::testInheritanceAndPolymorphism);
        runTest("12. Low Balance Alert Flagging (< Rs. 200)", Module2TestSuite::testLowBalanceAlertTrigger);

        System.out.println("===============================================================================");
        System.out.printf(" TEST SUMMARY: Total: %d | Passed: %d | Failed: %d\n",
                (testsPassed + testsFailed), testsPassed, testsFailed);
        System.out.println("===============================================================================");
        if (testsFailed > 0) {
            System.err.println("SOME TESTS FAILED! Check error logs above.");
            System.exit(1);
        } else {
            System.out.println("ALL MODULE 2 TESTS PASSED PERFECTLY!");
        }
    }

    private static void runTest(String testName, TestCase test) {
        try {
            test.run();
            System.out.printf(" [PASS] %s\n", testName);
            testsPassed++;
        } catch (Throwable t) {
            System.err.printf(" [FAIL] %s - Reason: %s\n", testName, t.getMessage());
            t.printStackTrace(System.err);
            testsFailed++;
        }
    }

    private static IAccountService createFreshService() {
        return new AccountServiceImpl(new InMemoryVehicleRepository(), new InMemoryFASTagAccountRepository());
    }

    // --- Test 1: Unique Vehicle Registration ---
    private static void testUniqueVehicleRegistration() {
        IAccountService service = createFreshService();
        service.registerVehicle(VehicleType.CAR, "MH-12-AB-1234", "Aakash", "9800000001");

        try {
            service.registerVehicle(VehicleType.CAR, "MH-12-AB-1234", "Duplicate Owner", "9800000002");
            throw new AssertionError("Expected DuplicateVehicleException was not thrown!");
        } catch (DuplicateVehicleException expected) {
            // Expected
        }
    }

    // --- Test 2: Strict 1:1 Tag Association ---
    private static void testOneToOneTagAssociation() {
        IAccountService service = createFreshService();
        service.registerVehicle(VehicleType.CAR, "DL-01-AA-9999", "Rohit", "9800000003");
        service.issueFASTag("DL-01-AA-9999", "TAG-TEST-1", 500.00);

        try {
            service.issueFASTag("DL-01-AA-9999", "TAG-TEST-2", 500.00);
            throw new AssertionError("Expected VehicleAlreadyHasTagException was not thrown!");
        } catch (VehicleAlreadyHasTagException expected) {
            // Expected
        }
    }

    // --- Test 3: Auto-Block When Balance < Rs. 100 ---
    private static void testAutoBlockWhenBalanceBelow100() {
        IAccountService service = createFreshService();
        service.registerVehicle(VehicleType.CAR, "KA-05-MM-1122", "Sneha", "9800000004");
        service.issueFASTag("KA-05-MM-1122", "TAG-AUTOBLOCK", 150.00);

        // Deduct Rs. 60 -> Balance becomes Rs. 90 (< Rs. 100)
        DebitResult result = service.deductToll("TAG-AUTOBLOCK", 60.00);
        assertTrue(result.isSuccessful(), "Debit should succeed");
        assertEquals(90.00, result.getRemainingBalance(), 0.001, "Balance should be 90.00");
        assertTrue(result.isAutoBlocked(), "Account should trigger auto-blocked flag");

        FASTagAccount acc = service.findAccountByTagId("TAG-AUTOBLOCK").orElseThrow();
        assertEquals(AccountStatus.BLOCKED, acc.getStatus(), "Status should be BLOCKED");

        // Attempt next debit while blocked -> should be refused
        DebitResult blockedResult = service.deductToll("TAG-AUTOBLOCK", 50.00);
        assertFalse(blockedResult.isSuccessful(), "Debit on blocked account must be refused");
    }

    // --- Test 4: Minimum Recharge Limit (Rs. 100) ---
    private static void testRechargeMinimumLimit() {
        IAccountService service = createFreshService();
        service.registerVehicle(VehicleType.VAN, "TN-01-VV-3344", "Mani", "9800000005");
        service.issueFASTag("TN-01-VV-3344", "TAG-MINRECH", 500.00);

        // Rs. 99 recharge should fail
        RechargeResult r1 = service.rechargeAccount("TAG-MINRECH", 99.00);
        assertFalse(r1.isSuccessful(), "Recharge below Rs. 100 must fail");

        // Rs. 100 recharge should succeed
        RechargeResult r2 = service.rechargeAccount("TAG-MINRECH", 100.00);
        assertTrue(r2.isSuccessful(), "Recharge of Rs. 100 must succeed");
        assertEquals(600.00, r2.getNewBalance(), 0.001, "Balance should be 600.00");
    }

    // --- Test 5: Maximum Recharge Limit (Rs. 10,000) ---
    private static void testRechargeMaximumLimit() {
        IAccountService service = createFreshService();
        service.registerVehicle(VehicleType.BUS, "GJ-01-BB-5566", "Chirag", "9800000006");
        service.issueFASTag("GJ-01-BB-5566", "TAG-MAXRECH", 1000.00);

        // Rs. 10,001 recharge should fail
        RechargeResult r1 = service.rechargeAccount("TAG-MAXRECH", 10001.00);
        assertFalse(r1.isSuccessful(), "Recharge above Rs. 10,000 must fail");

        // Rs. 10,000 recharge should succeed
        RechargeResult r2 = service.rechargeAccount("TAG-MAXRECH", 10000.00);
        assertTrue(r2.isSuccessful(), "Recharge of Rs. 10,000 must succeed");
        assertEquals(11000.00, r2.getNewBalance(), 0.001, "Balance should be 11,000.00");
    }

    // --- Test 6: Maximum Balance Cap (Rs. 50,000) ---
    private static void testMaxBalanceLimit50000() {
        IAccountService service = createFreshService();
        service.registerVehicle(VehicleType.TRUCK, "HR-01-TT-7788", "Deepak", "9800000007");
        service.issueFASTag("HR-01-TT-7788", "TAG-CAP50K", 45000.00);

        // Recharging Rs. 6,000 would take balance to 51,000 -> must fail
        RechargeResult r1 = service.rechargeAccount("TAG-CAP50K", 6000.00);
        assertFalse(r1.isSuccessful(), "Recharge exceeding Rs. 50,000 total balance cap must fail");

        // Recharging Rs. 5,000 takes balance to exact 50,000 -> should succeed
        RechargeResult r2 = service.rechargeAccount("TAG-CAP50K", 5000.00);
        assertTrue(r2.isSuccessful(), "Recharge reaching exact Rs. 50,000 cap must succeed");
        assertEquals(50000.00, r2.getNewBalance(), 0.001, "Balance should be 50,000.00");
    }

    // --- Test 7: Auto-Unblock on Recharge ---
    private static void testAutoUnblockOnRecharge() {
        IAccountService service = createFreshService();
        service.registerVehicle(VehicleType.CAR, "UP-32-UU-9988", "Kavita", "9800000008");
        // Initial balance < 100 -> starts BLOCKED
        service.issueFASTag("UP-32-UU-9988", "TAG-AUTOUNBLOCK", 80.00);

        FASTagAccount acc = service.findAccountByTagId("TAG-AUTOUNBLOCK").orElseThrow();
        assertEquals(AccountStatus.BLOCKED, acc.getStatus(), "Initial status must be BLOCKED");

        // Recharge with Rs. 100 -> New balance = Rs. 180 (>= Rs. 100) -> Auto-unblock
        RechargeResult r = service.rechargeAccount("TAG-AUTOUNBLOCK", 100.00);
        assertTrue(r.isSuccessful(), "Recharge should succeed");
        assertTrue(r.isAutoUnblocked(), "Account must report autoUnblocked = true");
        assertEquals(AccountStatus.ACTIVE, acc.getStatus(), "Status must automatically transition to ACTIVE");
    }

    // --- Test 8: Non-Negative Balance Constraint ---
    private static void testNonNegativeBalanceConstraint() {
        IAccountService service = createFreshService();
        service.registerVehicle(VehicleType.CAR, "RJ-14-RR-1234", "Raj", "9800000009");
        service.issueFASTag("RJ-14-RR-1234", "TAG-NONEG", 120.00);

        // Attempting to deduct Rs. 200 when balance is 120 must be refused
        DebitResult r = service.deductToll("TAG-NONEG", 200.00);
        assertFalse(r.isSuccessful(), "Debit exceeding balance must fail");
        assertEquals(120.00, r.getRemainingBalance(), 0.001, "Balance must remain unchanged at 120.00");
    }

    // --- Test 9: Admin Blacklist & Refusal ---
    private static void testAdminBlacklistRefusal() {
        IAccountService service = createFreshService();
        service.registerVehicle(VehicleType.CAR, "WB-01-WW-4321", "Subhash", "9800000010");
        service.issueFASTag("WB-01-WW-4321", "TAG-BLACKLIST", 2000.00);

        service.blacklistTag("TAG-BLACKLIST", "Reported stolen vehicle");
        FASTagAccount acc = service.findAccountByTagId("TAG-BLACKLIST").orElseThrow();
        assertTrue(acc.isBlacklisted(), "Tag must be marked as blacklisted");

        DebitResult r = service.deductToll("TAG-BLACKLIST", 100.00);
        assertFalse(r.isSuccessful(), "Blacklisted tag must be refused at toll plaza");
        assertTrue(r.getMessage().contains("BLACKLISTED"), "Error message should mention blacklisting");

        // Unblacklist
        service.unblacklistTag("TAG-BLACKLIST");
        assertFalse(acc.isBlacklisted(), "Tag should no longer be blacklisted");
        DebitResult rAfter = service.deductToll("TAG-BLACKLIST", 100.00);
        assertTrue(rAfter.isSuccessful(), "Unblacklisted tag should now be accepted");
    }

    // --- Test 10: Dispute Refund Credit & State Unblock ---
    private static void testDisputeRefundCredit() {
        IAccountService service = createFreshService();
        service.registerVehicle(VehicleType.CAR, "KL-07-KK-6677", "Faisal", "9800000011");
        service.issueFASTag("KL-07-KK-6677", "TAG-DISPUTE", 50.00); // Blocked

        // Dispute approved for wrong deduction of Rs. 100
        boolean unblocked = service.refundAccount("TAG-DISPUTE", 100.00, "Approved dispute for double billing at Plaza #1");
        assertTrue(unblocked, "Refund taking balance to Rs. 150 should unblock account");

        FASTagAccount acc = service.findAccountByTagId("TAG-DISPUTE").orElseThrow();
        assertEquals(150.00, acc.getBalance(), 0.001, "Balance should be 150.00");
        assertEquals(AccountStatus.ACTIVE, acc.getStatus(), "Account should now be ACTIVE");
    }

    // --- Test 11: Inheritance & Polymorphism ---
    private static void testInheritanceAndPolymorphism() {
        Vehicle car = new Car("MH-01-1111", "Tester 1", "9000000001");
        Vehicle van = new Van("MH-01-2222", "Tester 2", "9000000002");
        Vehicle bus = new Bus("MH-01-3333", "Tester 3", "9000000003");
        Vehicle truck = new Truck("MH-01-4444", "Tester 4", "9000000004");
        Vehicle mav = new MultiAxleVehicle("MH-01-5555", "Tester 5", "9000000005", 5);

        assertEquals(2, car.getAxleCount(), "Car axles should be 2");
        assertEquals(2, van.getAxleCount(), "Van axles should be 2");
        assertEquals(2, bus.getAxleCount(), "Bus axles should be 2");
        assertEquals(2, truck.getAxleCount(), "Truck axles should be 2");
        assertEquals(5, mav.getAxleCount(), "Multi-axle vehicle axles should be 5");

        assertTrue(car.getCategoryDescription().contains("Passenger Car"), "Car category check");
        assertTrue(mav.getCategoryDescription().contains("5 axles"), "MAV description check");
    }

    // --- Test 12: Low Balance Alert Flagging (< Rs. 200) ---
    private static void testLowBalanceAlertTrigger() {
        IAccountService service = createFreshService();
        service.registerVehicle(VehicleType.CAR, "MP-09-MM-7890", "Nitin", "9800000012");
        service.issueFASTag("MP-09-MM-7890", "TAG-LOWBAL", 300.00);

        // Deduct 150 -> balance becomes 150 (< 200 threshold)
        DebitResult result = service.deductToll("TAG-LOWBAL", 150.00);
        assertTrue(result.isSuccessful(), "Debit should succeed");
        assertTrue(result.isLowBalanceAlertTriggered(), "Should trigger low balance alert (< Rs. 200)");
        assertFalse(result.isAutoBlocked(), "Should NOT be auto-blocked yet because balance (150) >= 100");
    }

    // --- Assert Helpers ---
    private static void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError("ASSERTION FAILED: " + message);
    }

    private static void assertFalse(boolean condition, String message) {
        if (condition) throw new AssertionError("ASSERTION FAILED: " + message);
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(String.format("ASSERTION FAILED: %s. Expected <%s>, but was <%s>", message, expected, actual));
        }
    }

    private static void assertEquals(double expected, double actual, double delta, String message) {
        if (Math.abs(expected - actual) > delta) {
            throw new AssertionError(String.format("ASSERTION FAILED: %s. Expected <%.2f>, but was <%.2f>", message, expected, actual));
        }
    }

    @FunctionalInterface
    private interface TestCase {
        void run() throws Exception;
    }
}
