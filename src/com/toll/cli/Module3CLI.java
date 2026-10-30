package com.toll.cli;

import com.toll.common.VehicleType;
import com.toll.module2.account.service.IAccountService;
import com.toll.module3.tollengine.ITollProcessingService;
import com.toll.module3.tollengine.ITollProcessingService.CrossingResult;
import com.toll.module3.tollengine.TollProcessingServiceImpl;
import com.toll.module3.tollengine.TollTransaction;
import com.toll.test.Module3TestSuite;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

/**
 * Interactive Console CLI for Module 3: Toll Processing Engine.
 * Simulates RFID crossings, boom barrier controls, AVC mismatch detection,
 * emergency vehicle bypass, and cash fallbacks.
 */
public class Module3CLI {
    private final ITollProcessingService tollEngine;
    private final IAccountService accountService;
    private final Scanner scanner;

    public Module3CLI(IAccountService accountService) {
        this.accountService = accountService;
        this.tollEngine = new TollProcessingServiceImpl(accountService);
        this.scanner = new Scanner(System.in);
    }

    public Module3CLI() {
        this.tollEngine = new TollProcessingServiceImpl();
        this.accountService = null;
        this.scanner = new Scanner(System.in);
    }

    public void start() {
        printBanner();
        boolean running = true;
        while (running) {
            printMainMenu();
            System.out.print(" Select an option [1-9]: ");
            String choice = scanner.nextLine().trim();
            System.out.println();

            switch (choice) {
                case "1" -> handleStandardFASTagCrossing();
                case "2" -> handleAntiDoubleBillingSimulation();
                case "3" -> handleAVCMismatchSimulation();
                case "4" -> handleEmergencyLaneSimulation();
                case "5" -> handleMissingTagPenaltySimulation();
                case "6" -> handleInsufficientBalanceSimulation();
                case "7" -> handleViewAuditJournal();
                case "8" -> handleRunTestSuite();
                case "9" -> {
                    System.out.println("Returning to Toll Management System Main Menu...");
                    running = false;
                }
                default -> System.out.println("Invalid option. Please choose between 1 and 9.");
            }
            if (running) {
                System.out.println("\nPress [ENTER] to continue...");
                scanner.nextLine();
            }
        }
    }

    private void printBanner() {
        System.out.println("=========================================================================================");
        System.out.println("     NATIONAL HIGHWAYS AUTHORITY OF INDIA (NHAI) - TOLL MANAGEMENT SYSTEM (FASTag)     ");
        System.out.println("                     MODULE 3: TOLL PROCESSING & EDGE ENGINE                            ");
        System.out.println("=========================================================================================");
        System.out.println(" Active Plaza: #1 (Khedshivapur Plaza - NH-48) | Boom Barrier Actuators: READY");
    }

    private void printMainMenu() {
        System.out.println("\n----------------------------- MAIN MENU (MODULE 3) -----------------------------");
        System.out.println(" [1] Simulate FASTag Lane Crossing (RFID scan, balance debit, barrier actuator)");
        System.out.println(" [2] Simulate 2-Minute Anti-Tailgating & Anti-Double-Billing Cooldown");
        System.out.println(" [3] Simulate Optical AVC Sensor Mismatch (Higher toll charged & flagged)");
        System.out.println(" [4] Simulate Emergency Lane Clearance (Ambulance Rs. 0 vs Private Car Refusal)");
        System.out.println(" [5] Simulate Missing Tag in FASTag Lane (Diverted for Double Toll 2x Penalty)");
        System.out.println(" [6] Simulate Insufficient Balance in FASTag Lane (Diverted for Normal Toll 1x)");
        System.out.println(" [7] View Audit Transaction Journal (Tabular Log & Review Flags)");
        System.out.println(" [8] Run Automated Module 3 Verification Test Suite (15 Tests)");
        System.out.println(" [9] Exit to Top-Level Menu");
        System.out.println("--------------------------------------------------------------------------------");
    }

    private void handleStandardFASTagCrossing() {
        System.out.println("--- [1] Standard FASTag Crossing ---");
        System.out.print("Enter FASTag ID (e.g. IND-TAG-1001) [Press ENTER for IND-TAG-1001]: ");
        String tag = scanner.nextLine().trim();
        if (tag.isEmpty()) tag = "IND-TAG-1001";

        CrossingResult result = tollEngine.processCrossing(1, 1, tag, VehicleType.CAR);
        displayCrossingResult(result);
    }

    private void handleAntiDoubleBillingSimulation() {
        System.out.println("--- [2] 2-Minute Anti-Double-Billing Cooldown Simulation ---");
        String tag = "IND-TAG-1001";
        LocalDateTime now = LocalDateTime.now();

        System.out.println("Attempting 1st crossing at " + now.format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "...");
        CrossingResult res1 = tollEngine.processCrossing(1, 1, tag, VehicleType.CAR, now);
        displayCrossingResult(res1);

        LocalDateTime secondAttempt = now.plusSeconds(30);
        System.out.println("\nAttempting 2nd crossing 30 seconds later at " + 
                secondAttempt.format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "...");
        CrossingResult res2 = tollEngine.processCrossing(1, 1, tag, VehicleType.CAR, secondAttempt);
        displayCrossingResult(res2);
    }

    private void handleAVCMismatchSimulation() {
        System.out.println("--- [3] Optical AVC Sensor Mismatch Simulation ---");
        System.out.println("Scenario: FASTag registered as CAR (Base Rate: Rs. 80.00)");
        System.out.println("Optical AVC Sensor detects: TRUCK (Base Rate: Rs. 270.00)");

        CrossingResult result = tollEngine.processCrossing(1, 1, "IND-TAG-1001", VehicleType.TRUCK, LocalDateTime.now().plusMinutes(5));
        displayCrossingResult(result);
    }

    private void handleEmergencyLaneSimulation() {
        System.out.println("--- [4] Emergency Lane Clearance Simulation ---");
        System.out.println("Scenario A: Ambulance (MH-12-AMB-108) entering Emergency Lane #9:");
        CrossingResult resAmb = tollEngine.processEmergencyCrossing(1, 9, "MH-12-AMB-108");
        displayCrossingResult(resAmb);

        System.out.println("\nScenario B: Private Vehicle (MH-12-PVT-0007) entering Emergency Lane #9:");
        CrossingResult resPvt = tollEngine.processEmergencyCrossing(1, 9, "MH-12-PVT-0007");
        displayCrossingResult(resPvt);
    }

    private void handleMissingTagPenaltySimulation() {
        System.out.println("--- [5] Missing FASTag in FASTag Lane Simulation ---");
        System.out.println("Vehicle enters FASTag Lane #1 without any RFID tag.");

        CrossingResult res = tollEngine.processCrossing(1, 1, null, VehicleType.CAR);
        displayCrossingResult(res);

        if (res.isDivertedToCash()) {
            System.out.println("\n-> Vehicle diverted to Cash Lane #2. Settle with operator Suresh:");
            CrossingResult cashRes = tollEngine.processDivertedVehicleInCash(res, 2, "Operator-Suresh");
            displayCrossingResult(cashRes);
        }
    }

    private void handleInsufficientBalanceSimulation() {
        System.out.println("--- [6] Insufficient Balance Simulation ---");
        System.out.println("Using Account IND-TAG-1002 (Balance Rs. 80.00, Auto-Blocked):");

        CrossingResult res = tollEngine.processCrossing(1, 1, "IND-TAG-1002", VehicleType.CAR);
        displayCrossingResult(res);

        if (res.isDivertedToCash()) {
            System.out.println("\n-> Vehicle diverted to Cash Lane #2. Settle Normal Toll with operator Ramesh:");
            CrossingResult cashRes = tollEngine.processDivertedVehicleInCash(res, 2, "Operator-Ramesh");
            displayCrossingResult(cashRes);
        }
    }

    private void handleViewAuditJournal() {
        System.out.println("--- [7] Toll Transaction Audit Journal ---");
        List<TollTransaction> list = tollEngine.getAllTransactions();
        if (list.isEmpty()) {
            System.out.println("No transactions recorded yet.");
            return;
        }

        System.out.printf("%-10s | %-13s | %-5s | %-8s | %-7s | %-8s | %-7s | %-10s | %s%n",
                "TXN ID", "VEHICLE NO", "PLAZA", "AMOUNT", "MODE", "STATUS", "FLAGGED", "TIME", "DETAILS");
        System.out.println("---------------------------------------------------------------------------------------------------------");

        DateTimeFormatter tf = DateTimeFormatter.ofPattern("HH:mm:ss");
        for (TollTransaction tx : list) {
            String shortId = tx.getTransactionId().length() > 8 ? tx.getTransactionId().substring(0, 8) : tx.getTransactionId();
            String reason = tx.getFailureReason() != null ? "Reason: " + tx.getFailureReason() : 
                    (tx.getOperatorName() != null ? "Op: " + tx.getOperatorName() : "Trip: " + tx.getTripType());

            System.out.printf("%-10s | %-13s | #%-4d | Rs.%-6.2f | %-7s | %-8s | %-7s | %-10s | %s%n",
                    shortId, tx.getVehicleRegNo(), tx.getPlazaId(), tx.getTollAmount(),
                    tx.getPaymentMethod(), tx.getStatus(), tx.isFlaggedForReview() ? "YES(AVC)" : "NO",
                    tx.getTimestamp().format(tf), reason);
        }
    }

    private void handleRunTestSuite() {
        System.out.println("--- [8] Running Automated Module 3 Test Suite ---");
        Module3TestSuite.main(new String[0]);
    }

    private void displayCrossingResult(CrossingResult result) {
        System.out.println("--------------------------------------------------------------------------------");
        System.out.println(" STATUS: " + result.status() + " | Boom Barrier: " + 
                (result.barrierOpened() ? "OPEN (Raised)" : "CLOSED (Lowered)"));
        System.out.println(" Message: " + result.message());
        if (result.flaggedForReview()) {
            System.out.println(" AUDIT WARNING: Transaction flagged for administrative review (AVC Mismatch)!");
        }
        System.out.println("--------------------------------------------------------------------------------");
    }
}
