package com.toll.cli;

import com.toll.common.*;
import com.toll.module1.plaza.IPlazaService;
import com.toll.module1.plaza.PlazaServiceImpl;
import com.toll.module1.plaza.model.Lane;
import com.toll.module1.plaza.model.TollPlaza;
import com.toll.module1.plaza.model.TollRate;
import com.toll.module1.plaza.repository.InMemoryPlazaRepository;
import com.toll.module2.account.model.FASTagAccount;
import com.toll.module2.account.model.Vehicle;
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
import com.toll.module5.billing.model.CashPaymentRecord;
import com.toll.module5.billing.model.Dispute;
import com.toll.module5.billing.model.LowBalanceAlert;
import com.toll.module5.billing.model.RechargeRecord;
import com.toll.test.AllModulesTestSuite;
import com.toll.test.Module2TestSuite;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Master Integrated Interactive Console Application for the Toll Management System (FASTag).
 * Unites all 5 modules into a seamless terminal interface for administration,
 * live toll crossings, billing, disputes, and analytics.
 */
public class MainCLI {
    private final IPlazaService plazaService;
    private final IAccountService accountService;
    private final IPricingService pricingService;
    private final IBillingReportingService billingService;
    private final ITransactionRepository transactionRepository;
    private final ITollProcessingService tollService;

    private final Scanner scanner;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public MainCLI() {
        InMemoryPlazaRepository plazaRepo = new InMemoryPlazaRepository();
        this.plazaService = new PlazaServiceImpl(plazaRepo);

        this.accountService = new AccountServiceImpl(
                new InMemoryVehicleRepository(),
                new InMemoryFASTagAccountRepository()
        );

        InMemoryPassRepository passRepo = new InMemoryPassRepository();
        this.pricingService = new PricingServiceImpl(passRepo);

        this.transactionRepository = new InMemoryTransactionRepository();

        this.billingService = new BillingReportingServiceImpl(
                this.accountService,
                this.transactionRepository,
                plazaRepo
        );

        this.tollService = new TollProcessingServiceImpl(
                this.plazaService,
                this.accountService,
                this.pricingService,
                this.billingService,
                this.transactionRepository
        );

        this.scanner = new Scanner(System.in);

        // Preload realistic Indian Highway system state
        DataSeeder.seedAllModules(plazaService, accountService, pricingService, billingService, transactionRepository);
    }

    public void start() {
        printBanner();
        boolean running = true;
        while (running) {
            printMainMenu();
            System.out.print(" Select Module / Operation [1-8]: ");
            String choice = scanner.nextLine().trim();
            System.out.println();

            switch (choice) {
                case "1" -> handleModule1Menu();
                case "2" -> handleModule2Menu();
                case "3" -> handleModule3Menu();
                case "4" -> handleModule4Menu();
                case "5" -> handleModule5Menu();
                case "6" -> handleModule6Analytics();
                case "7" -> handleRunTestSuites();
                case "8" -> {
                    System.out.println("Exiting Toll Management System (FASTag). Safe Travels on Indian Highways!");
                    running = false;
                }
                default -> System.out.println("Invalid choice. Please select an option between 1 and 8.");
            }
            if (running) {
                System.out.println("\nPress [ENTER] to return to Main Menu...");
                scanner.nextLine();
            }
        }
    }

    private void printBanner() {
        System.out.println("=========================================================================================");
        System.out.println("     NATIONAL HIGHWAYS AUTHORITY OF INDIA (NHAI) - TOLL MANAGEMENT SYSTEM (FASTag)     ");
        System.out.println("                 COMPLETE 5-MODULE INTEGRATED OBJECT-ORIENTED SYSTEM                    ");
        System.out.println("=========================================================================================");
        System.out.println(" System Status: ACTIVE | Pre-seeded with Indian Highway Plazas, FASTags, Passes & Rates.");
    }

    private void printMainMenu() {
        System.out.println("\n-------------------------------- MASTER APPLICATION MENU --------------------------------");
        System.out.println(" [1] [PLAZA]    MODULE 1: Toll Plaza & Lane Administration (Plazas, Lanes, Rates)");
        System.out.println(" [2] [FASTAG]   MODULE 2: Vehicle & FASTag Accounts (1:1 Links, Auto-Block, Blacklist)");
        System.out.println(" [3] [CROSSING] MODULE 3: Live Toll Plaza Crossing Simulator (RFID, AVC Check, Fallback)");
        System.out.println(" [4] [PRICING]  MODULE 4: Rates, Discounts & Monthly Passes (24h Return 50%, Nearest Rs. 5)");
        System.out.println(" [5] [BILLING]  MODULE 5: Billing, Recharges, Disputes & Alerts (UPI/Card, 7-Day Dispute)");
        System.out.println(" [6] [REPORTS]  EXECUTIVE REPORTS: Daily Revenue, Lane Traffic & SQL Queries 1, 2, 3");
        System.out.println(" [7] [TESTS]    AUTOMATED TEST SUITE: Run 19 End-to-End System Integration Tests");
        System.out.println(" [8] [EXIT]     Exit Application");
        System.out.println("-----------------------------------------------------------------------------------------");
    }

    // =========================================================================================
    // MODULE 1: PLAZA & LANES
    // =========================================================================================
    private void handleModule1Menu() {
        System.out.println("=== [1] MODULE 1: Toll Plaza & Lane Administration ===");
        System.out.println(" 1. View All Toll Plazas & Lanes");
        System.out.println(" 2. Create New Toll Plaza");
        System.out.println(" 3. Add Lane to Plaza");
        System.out.println(" 4. Configure Toll Rate with Effective Date");
        System.out.println(" 5. Open / Close Toll Plaza (Enforces >=2 lanes & all 5 vehicle rates)");
        System.out.println(" 6. Open / Close Lane (Enforces lane safety busy lock)");
        System.out.print("Select action [1-6]: ");
        String act = scanner.nextLine().trim();

        switch (act) {
            case "1" -> viewAllPlazas();
            case "2" -> createPlaza();
            case "3" -> addLane();
            case "4" -> setRate();
            case "5" -> togglePlazaStatus();
            case "6" -> toggleLaneStatus();
            default -> System.out.println("Invalid selection.");
        }
    }

    private void viewAllPlazas() {
        List<TollPlaza> plazas = plazaService.getAllPlazas();
        System.out.println("\n--- Registered Toll Plazas ---");
        for (TollPlaza p : plazas) {
            System.out.printf("\nPlaza #%d: %s | Highway: %s | Location: %s | Status: %s\n",
                    p.getPlazaId(), p.getName(), p.getHighwayName(), p.getLocation(), p.getStatus());
            System.out.println("  Lanes (" + p.getLanes().size() + "):");
            for (Lane l : p.getLanes()) {
                System.out.printf("   - Lane #%d: %-10s | Status: %-6s | Busy: %s (%s)\n",
                        l.getLaneNumber(), l.getLaneType(), l.getStatus(), l.isProcessingVehicle() ? "YES" : "NO", l.getLaneDescription());
            }
            System.out.println("  Active Single Toll Rates (Today):");
            for (VehicleType vt : VehicleType.values()) {
                p.getActiveRate(vt, TripType.SINGLE, LocalDate.now())
                        .ifPresent(r -> System.out.printf("   * %-16s: Rs. %.2f\n", vt.getDisplayName(), r));
            }
        }
    }

    private void createPlaza() {
        System.out.print("Enter unique Plaza ID (e.g. 4): ");
        try {
            int id = Integer.parseInt(scanner.nextLine().trim());
            System.out.print("Enter Plaza Name: ");
            String name = scanner.nextLine().trim();
            System.out.print("Enter Highway Name (e.g. NH-44): ");
            String highway = scanner.nextLine().trim();
            System.out.print("Enter Location: ");
            String loc = scanner.nextLine().trim();

            TollPlaza plaza = plazaService.createPlaza(id, name, highway, loc);
            System.out.println("SUCCESS: Plaza created (Initial status: INACTIVE). Add >= 2 lanes and rates before opening.");
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
        }
    }

    private void addLane() {
        System.out.print("Enter Plaza ID: ");
        try {
            int pid = Integer.parseInt(scanner.nextLine().trim());
            System.out.print("Enter Lane Number: ");
            int lnum = Integer.parseInt(scanner.nextLine().trim());
            System.out.print("Enter Lane Type (FASTAG / CASH / EMERGENCY): ");
            String type = scanner.nextLine().trim();

            plazaService.addLane(pid, lnum, type);
            System.out.println("SUCCESS: Lane #" + lnum + " added to Plaza #" + pid);
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
        }
    }

    private void setRate() {
        System.out.print("Enter Plaza ID: ");
        try {
            int pid = Integer.parseInt(scanner.nextLine().trim());
            System.out.println("Select Vehicle Type: 1.CAR 2.VAN 3.BUS 4.TRUCK 5.MULTI_AXLE");
            int vtChoice = Integer.parseInt(scanner.nextLine().trim());
            VehicleType vt = VehicleType.values()[vtChoice - 1];

            System.out.print("Enter Single Trip Rate (Rs.): ");
            double rate = Double.parseDouble(scanner.nextLine().trim());

            System.out.print("Enter Effective Date (YYYY-MM-DD) or press ENTER for today: ");
            String dtStr = scanner.nextLine().trim();
            LocalDate effDate = dtStr.isEmpty() ? LocalDate.now() : LocalDate.parse(dtStr, DATE_FMT);

            plazaService.setTollRate(pid, vt, TripType.SINGLE, rate, effDate);
            System.out.println("SUCCESS: Rate configured for " + vt + " (Effective from: " + effDate + ")");
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
        }
    }

    private void togglePlazaStatus() {
        System.out.print("Enter Plaza ID: ");
        try {
            int pid = Integer.parseInt(scanner.nextLine().trim());
            System.out.print("Choose: 1. Open Plaza  2. Close Plaza: ");
            String ch = scanner.nextLine().trim();
            if ("1".equals(ch)) {
                boolean ok = plazaService.openPlaza(pid);
                if (ok) {
                    System.out.println("SUCCESS: Plaza #" + pid + " is now OPEN/ACTIVE.");
                } else {
                    System.err.println("REJECTED: Plaza must have at least 2 lanes AND rates configured for all 5 vehicle types!");
                }
            } else if ("2".equals(ch)) {
                plazaService.closePlaza(pid);
                System.out.println("Plaza #" + pid + " is now CLOSED.");
            }
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
        }
    }

    private void toggleLaneStatus() {
        System.out.print("Enter Plaza ID: ");
        try {
            int pid = Integer.parseInt(scanner.nextLine().trim());
            System.out.print("Enter Lane Number: ");
            int lnum = Integer.parseInt(scanner.nextLine().trim());
            System.out.print("Choose: 1. Open Lane  2. Close Lane: ");
            String ch = scanner.nextLine().trim();
            if ("1".equals(ch)) {
                plazaService.openLane(pid, lnum);
                System.out.println("SUCCESS: Lane #" + lnum + " is now OPEN.");
            } else if ("2".equals(ch)) {
                boolean closed = plazaService.closeLane(pid, lnum);
                if (closed) {
                    System.out.println("SUCCESS: Lane #" + lnum + " is now CLOSED.");
                } else {
                    System.err.println("REJECTED: Lane safety lock! Cannot close lane while a vehicle is being processed.");
                }
            }
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
        }
    }

    // =========================================================================================
    // MODULE 2: VEHICLES & FASTAG
    // =========================================================================================
    private void handleModule2Menu() {
        System.out.println("=== [2] MODULE 2: Vehicle & FASTag Accounts ===");
        System.out.println(" 1. Register New Vehicle");
        System.out.println(" 2. Issue & Link FASTag (Strict 1:1 validation)");
        System.out.println(" 3. View All Vehicles & Linked Accounts");
        System.out.println(" 4. Search Account / Vehicle");
        System.out.println(" 5. Admin Blacklist / Unblacklist FASTag");
        System.out.print("Select action [1-5]: ");
        String act = scanner.nextLine().trim();

        switch (act) {
            case "1" -> registerVehicle();
            case "2" -> issueTag();
            case "3" -> viewAllVehiclesTable();
            case "4" -> searchAccount();
            case "5" -> adminBlacklist();
            default -> System.out.println("Invalid selection.");
        }
    }

    private void registerVehicle() {
        System.out.println("Select Vehicle Type: 1.Car 2.Van 3.Bus 4.Truck 5.Multi-Axle");
        try {
            int c = Integer.parseInt(scanner.nextLine().trim());
            VehicleType vt = VehicleType.values()[c - 1];

            System.out.print("Registration Number (e.g. MH-14-GH-1234): ");
            String reg = scanner.nextLine().trim();
            System.out.print("Owner Name: ");
            String owner = scanner.nextLine().trim();
            System.out.print("Phone Number: ");
            String phone = scanner.nextLine().trim();

            Vehicle v;
            if (vt == VehicleType.MULTI_AXLE) {
                System.out.print("Axle count (min 3): ");
                int ax = Integer.parseInt(scanner.nextLine().trim());
                v = accountService.registerMultiAxleVehicle(reg, owner, phone, ax);
            } else {
                v = accountService.registerVehicle(vt, reg, owner, phone);
            }
            System.out.println("SUCCESS: Registered " + v);
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
        }
    }

    private void issueTag() {
        System.out.print("Enter Registered Vehicle Reg Number: ");
        String reg = scanner.nextLine().trim();
        System.out.print("Enter New Tag ID (e.g. IND-TAG-9901): ");
        String tag = scanner.nextLine().trim();
        System.out.print("Initial Deposit Balance (Rs. 100 - Rs. 50,000): ");
        try {
            double bal = Double.parseDouble(scanner.nextLine().trim());
            FASTagAccount acc = accountService.issueFASTag(reg, tag, bal);
            System.out.println("SUCCESS: Issued FASTag " + acc.getTagId() + " for " + acc.getVehicleRegistrationNumber());
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
        }
    }

    private void viewAllVehiclesTable() {
        List<Vehicle> list = accountService.getAllVehicles();
        System.out.println("+" + "-".repeat(16) + "+" + "-".repeat(12) + "+" + "-".repeat(20) + "+" + "-".repeat(16) + "+" + "-".repeat(12) + "+" + "-".repeat(10) + "+" + "-".repeat(14) + "+");
        System.out.printf("| %-14s | %-10s | %-18s | %-14s | %-10s | %-8s | %-12s |\n",
                "REG NUMBER", "TYPE", "OWNER", "TAG ID", "BALANCE", "STATUS", "BLACKLISTED");
        System.out.println("+" + "-".repeat(16) + "+" + "-".repeat(12) + "+" + "-".repeat(20) + "+" + "-".repeat(16) + "+" + "-".repeat(12) + "+" + "-".repeat(10) + "+" + "-".repeat(14) + "+");

        for (Vehicle v : list) {
            Optional<FASTagAccount> accOpt = accountService.findAccountByVehicleRegistration(v.getRegistrationNumber());
            String tagId = accOpt.map(FASTagAccount::getTagId).orElse("(No Tag)");
            String balance = accOpt.map(a -> String.format("Rs.%.2f", a.getBalance())).orElse("N/A");
            String status = accOpt.map(a -> a.getStatus().name()).orElse("N/A");
            String blacklisted = accOpt.map(a -> a.isBlacklisted() ? "YES" : "NO").orElse("NO");

            System.out.printf("| %-14s | %-10s | %-18s | %-14s | %-10s | %-8s | %-12s |\n",
                    v.getRegistrationNumber(), v.getVehicleType().name(),
                    truncate(v.getOwnerName(), 18), tagId, balance, status, blacklisted);
        }
        System.out.println("+" + "-".repeat(16) + "+" + "-".repeat(12) + "+" + "-".repeat(20) + "+" + "-".repeat(16) + "+" + "-".repeat(12) + "+" + "-".repeat(10) + "+" + "-".repeat(14) + "+");
    }

    private void searchAccount() {
        System.out.print("Enter Tag ID or Vehicle Registration Number: ");
        String q = scanner.nextLine().trim();
        Optional<FASTagAccount> byTag = accountService.findAccountByTagId(q);
        Optional<FASTagAccount> accOpt = byTag.isPresent() ? byTag : accountService.findAccountByVehicleRegistration(q);

        if (accOpt.isPresent()) {
            FASTagAccount a = accOpt.get();
            System.out.println("\nFASTag Details:");
            System.out.println(" Tag ID:       " + a.getTagId());
            System.out.println(" Vehicle:      " + a.getVehicleRegistrationNumber());
            System.out.printf(" Balance:      Rs. %.2f\n", a.getBalance());
            System.out.println(" Status:       " + a.getStatus() + " (" + a.getStatus().getDescription() + ")");
            System.out.println(" Blacklisted:  " + (a.isBlacklisted() ? "YES (" + a.getBlacklistReason() + ")" : "NO"));
        } else {
            System.out.println("No matching record found.");
        }
    }

    private void adminBlacklist() {
        System.out.print("Enter FASTag ID: ");
        String tag = scanner.nextLine().trim();
        System.out.print("Choose: 1. Blacklist Tag  2. Unblacklist Tag: ");
        String ch = scanner.nextLine().trim();
        try {
            if ("1".equals(ch)) {
                System.out.print("Enter Reason: ");
                String reason = scanner.nextLine().trim();
                accountService.blacklistTag(tag, reason);
                System.out.println("SUCCESS: Tag " + tag + " has been BLACKLISTED. Refused at all toll plazas.");
            } else if ("2".equals(ch)) {
                accountService.unblacklistTag(tag);
                System.out.println("SUCCESS: Tag " + tag + " blacklist flag removed.");
            }
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
        }
    }

    // =========================================================================================
    // MODULE 3: LIVE TOLL SIMULATOR
    // =========================================================================================
    private void handleModule3Menu() {
        System.out.println("=== [3] MODULE 3: Live Toll Plaza Crossing Simulator ===");
        System.out.println(" 1. Simulate FASTag Lane Crossing (RFID Scan, AVC Check, Cooldown, Passes)");
        System.out.println(" 2. Simulate Cash Lane Crossing (Normal Toll or 2x Double Toll Penalty)");
        System.out.println(" 3. Simulate Emergency Lane Crossing (Ambulance / Fire Engine Rs. 0 Free Bypass)");
        System.out.println(" 4. View Recent Crossing Transactions");
        System.out.print("Select action [1-4]: ");
        String act = scanner.nextLine().trim();

        switch (act) {
            case "1" -> simulateFASTagCrossing();
            case "2" -> simulateCashCrossing();
            case "3" -> simulateEmergencyCrossing();
            case "4" -> viewRecentTransactions();
            default -> System.out.println("Invalid selection.");
        }
    }

    private void simulateFASTagCrossing() {
        System.out.print("Enter Plaza ID (1, 2, or 3): ");
        int pid = Integer.parseInt(scanner.nextLine().trim());
        System.out.print("Enter FASTag Lane Number (e.g. 1): ");
        int lane = Integer.parseInt(scanner.nextLine().trim());
        System.out.print("Enter FASTag ID to Scan (e.g. IND-TAG-1001, IND-TAG-1003): ");
        String tag = scanner.nextLine().trim();

        System.out.println("Optical AVC Camera Observed Vehicle Type:");
        System.out.println(" 1. CAR  2. VAN  3. BUS  4. TRUCK  5. MULTI_AXLE");
        System.out.print("Choose observed type [1-5]: ");
        int c = Integer.parseInt(scanner.nextLine().trim());
        VehicleType obs = VehicleType.values()[c - 1];

        var resp = tollService.processFASTagCrossing(pid, lane, tag, obs, LocalDateTime.now());

        System.out.println("\n----------------- CROSSING PIPELINE RESULT -----------------");
        if (resp.barrierOpened()) {
            System.out.println(" >>> BOOM BARRIER OPENED! [GREEN LIGHT] <<<");
            System.out.println(" Message: " + resp.message());
            System.out.printf(" Billed Amount: Rs. %.2f | Trip: %s\n",
                    resp.transaction().tollAmount(), resp.transaction().tripType());
            if (resp.transaction().flaggedForReview()) {
                System.out.println(" [ALERT] FLAGGED FOR REVIEW: Observed vehicle type differed from registered tag type!");
            }
        } else {
            System.err.println(" >>> BOOM BARRIER REMAINS CLOSED! [RED LIGHT] <<<");
            System.err.println(" Reason: " + resp.message());
            if (resp.redirectedToCash()) {
                if (resp.isDoubleTollPenalty()) {
                    System.out.println(" Action: Divert to Cash Lane -> Pay DOUBLE TOLL (2x penalty) in Cash!");
                } else {
                    System.out.println(" Action: Divert to Cash Lane -> Pay NORMAL TOLL in Cash!");
                }
            }
        }
        System.out.println("------------------------------------------------------------");
    }

    private void simulateCashCrossing() {
        System.out.print("Enter Plaza ID (e.g. 1): ");
        int pid = Integer.parseInt(scanner.nextLine().trim());
        System.out.print("Enter Cash Lane Number (e.g. 3): ");
        int lane = Integer.parseInt(scanner.nextLine().trim());
        System.out.print("Enter Vehicle Registration Number: ");
        String reg = scanner.nextLine().trim();

        System.out.println("Select Vehicle Type: 1.CAR 2.VAN 3.BUS 4.TRUCK 5.MULTI_AXLE");
        int c = Integer.parseInt(scanner.nextLine().trim());
        VehicleType vt = VehicleType.values()[c - 1];

        System.out.print("Enter Lane Operator Name: ");
        String op = scanner.nextLine().trim();

        System.out.print("Is this a 2x Double Toll Penalty crossing (e.g. no FASTag in ETC lane)? (y/n): ");
        boolean isPenalty = scanner.nextLine().trim().equalsIgnoreCase("y");

        var resp = tollService.processCashCrossing(pid, lane, reg, vt, op, isPenalty, LocalDateTime.now());
        System.out.println("\n" + resp.message());
    }

    private void simulateEmergencyCrossing() {
        System.out.print("Enter Plaza ID (e.g. 1): ");
        int pid = Integer.parseInt(scanner.nextLine().trim());
        System.out.print("Enter Emergency Lane Number (e.g. 4): ");
        int lane = Integer.parseInt(scanner.nextLine().trim());
        System.out.print("Enter Vehicle Registration: ");
        String reg = scanner.nextLine().trim();
        System.out.print("Emergency Vehicle Type (Ambulance / Fire Engine): ");
        String type = scanner.nextLine().trim();

        var resp = tollService.processEmergencyCrossing(pid, lane, reg, type, LocalDateTime.now());
        System.out.println("\n" + resp.message());
    }

    private void viewRecentTransactions() {
        List<TollTransaction> txns = transactionRepository.findAll();
        System.out.println("\n--- Recent Crossing Transactions (" + txns.size() + ") ---");
        System.out.println("+" + "-".repeat(14) + "+" + "-".repeat(8) + "+" + "-".repeat(6) + "+" + "-".repeat(12) + "+" + "-".repeat(10) + "+" + "-".repeat(10) + "+" + "-".repeat(10) + "+" + "-".repeat(10) + "+");
        System.out.printf("| %-12s | %-6s | %-4s | %-10s | %-8s | %-8s | %-8s | %-8s |\n",
                "REG NO", "PLAZA", "LANE", "TAG ID", "AMOUNT", "TRIP", "STATUS", "FLAGGED");
        System.out.println("+" + "-".repeat(14) + "+" + "-".repeat(8) + "+" + "-".repeat(6) + "+" + "-".repeat(12) + "+" + "-".repeat(10) + "+" + "-".repeat(10) + "+" + "-".repeat(10) + "+" + "-".repeat(10) + "+");

        for (TollTransaction t : txns) {
            System.out.printf("| %-12s | #%-5d | #%-3d | %-10s | Rs.%-5.1f | %-8s | %-8s | %-8s |\n",
                    t.vehicleRegNo(), t.plazaId(), t.laneNumber(),
                    (t.tagId() == null ? "N/A" : t.tagId()),
                    t.tollAmount(), t.tripType().name(), t.status().name(),
                    t.flaggedForReview() ? "FLAGGED" : "NO");
        }
        System.out.println("+" + "-".repeat(14) + "+" + "-".repeat(8) + "+" + "-".repeat(6) + "+" + "-".repeat(12) + "+" + "-".repeat(10) + "+" + "-".repeat(10) + "+" + "-".repeat(10) + "+" + "-".repeat(10) + "+");
    }

    // =========================================================================================
    // MODULE 4: RATES & PASSES
    // =========================================================================================
    private void handleModule4Menu() {
        System.out.println("=== [4] MODULE 4: Rates, Discounts & Monthly Passes ===");
        System.out.println(" 1. Issue Monthly Pass for Plaza (Rs. 0 crossings)");
        System.out.println(" 2. View Active Monthly Passes");
        System.out.println(" 3. Test Fare Calculation (Simulate 24h Return Discount & Rounding)");
        System.out.print("Select action [1-3]: ");
        String act = scanner.nextLine().trim();

        switch (act) {
            case "1" -> issueMonthlyPass();
            case "2" -> viewPasses();
            case "3" -> testFareCalculation();
            default -> System.out.println("Invalid selection.");
        }
    }

    private void issueMonthlyPass() {
        System.out.print("Enter Vehicle Registration Number: ");
        String reg = scanner.nextLine().trim();
        System.out.print("Enter Plaza ID: ");
        int pid = Integer.parseInt(scanner.nextLine().trim());
        System.out.print("Enter Pass Fee (Rs.): ");
        double fee = Double.parseDouble(scanner.nextLine().trim());

        String passId = "PASS-P" + pid + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        LocalDate start = LocalDate.now();
        LocalDate end = start.plusDays(30);

        try {
            MonthlyPass pass = pricingService.issueMonthlyPass(passId, reg, pid, start, end, fee);
            System.out.println("SUCCESS: Monthly pass issued! " + pass);
            System.out.println("Vehicle pays Rs. 0.00 at Plaza #" + pid + " until " + end);
        } catch (Exception e) {
            System.err.println("REJECTED: " + e.getMessage());
        }
    }

    private void viewPasses() {
        List<MonthlyPass> passes = pricingService.getAllPasses();
        System.out.println("\n--- All Issued Monthly Passes (" + passes.size() + ") ---");
        for (MonthlyPass p : passes) {
            boolean active = p.isValidOn(LocalDate.now());
            System.out.printf("Pass #%s | Vehicle: %s | Plaza: #%d | Valid: %s to %s | Fee: Rs. %.2f | Active: %s\n",
                    p.getPassId(), p.getVehicleRegistrationNumber(), p.getPlazaId(),
                    p.getStartDate(), p.getEndDate(), p.getFeePaid(), active ? "YES" : "EXPIRED");
        }
    }

    private void testFareCalculation() {
        System.out.print("Enter Plaza ID (1, 2, or 3): ");
        int pid = Integer.parseInt(scanner.nextLine().trim());
        System.out.print("Enter Vehicle Reg Number: ");
        String reg = scanner.nextLine().trim();

        System.out.println("Select Vehicle Type: 1.CAR 2.VAN 3.BUS 4.TRUCK 5.MULTI_AXLE");
        int c = Integer.parseInt(scanner.nextLine().trim());
        VehicleType vt = VehicleType.values()[c - 1];

        System.out.print("Is this vehicle returning within 24 hours of Single trip? (y/n): ");
        boolean isReturn = scanner.nextLine().trim().equalsIgnoreCase("y");

        var res = pricingService.calculateFare(pid, reg, vt, vt, LocalDateTime.now(), isReturn, plazaService);
        System.out.println("\nFare Evaluation Breakdown:");
        System.out.println(" Base Single Rate:       Rs. " + res.baseFare());
        System.out.println(" Final Rounded Fare:     Rs. " + res.roundedFinalFare());
        System.out.println(" Trip Classification:    " + res.tripType());
        System.out.println(" Return Discount (50%):  " + (res.returnDiscountApplied() ? "YES" : "NO"));
        System.out.println(" Monthly Pass Honored:   " + (res.passApplied() ? "YES (Rs. 0)" : "NO"));
        System.out.println(" Notes: " + res.pricingNotes());
    }

    // =========================================================================================
    // MODULE 5: BILLING, DISPUTES & ALERTS
    // =========================================================================================
    private void handleModule5Menu() {
        System.out.println("=== [5] MODULE 5: Billing, Recharges, Disputes & Alerts ===");
        System.out.println(" 1. Recharge FASTag (UPI, Card, Net Banking)");
        System.out.println(" 2. File Deduction Dispute (Within 7 days)");
        System.out.println(" 3. Admin Dispute Review & Balance Refund");
        System.out.println(" 4. View Low Balance Alerts (< Rs. 200)");
        System.out.println(" 5. View Cash Payment Operator Journal");
        System.out.println(" 6. View Recharge Records");
        System.out.print("Select action [1-6]: ");
        String act = scanner.nextLine().trim();

        switch (act) {
            case "1" -> handleRecharge();
            case "2" -> handleFileDispute();
            case "3" -> handleReviewDisputes();
            case "4" -> viewLowBalanceAlerts();
            case "5" -> viewCashJournal();
            case "6" -> viewRecharges();
            default -> System.out.println("Invalid selection.");
        }
    }

    private void handleRecharge() {
        System.out.print("Enter FASTag ID: ");
        String tag = scanner.nextLine().trim();
        System.out.print("Enter Amount (Rs. 100 - Rs. 10,000): ");
        double amt = Double.parseDouble(scanner.nextLine().trim());
        System.out.println("Select Payment Method: 1.UPI  2.CARD  3.NET_BANKING");
        int m = Integer.parseInt(scanner.nextLine().trim());
        RechargeMethod rm = (m == 1) ? RechargeMethod.UPI : (m == 2 ? RechargeMethod.CARD : RechargeMethod.NET_BANKING);

        RechargeRecord rec = billingService.processRecharge(tag, amt, rm);
        if (rec.status() == RechargeStatus.PAID) {
            System.out.println("SUCCESS: Recharge successful via " + rm + "! Record ID: " + rec.rechargeId());
        } else {
            System.err.println("FAILED: " + rec.failureReason());
        }
    }

    private void handleFileDispute() {
        System.out.print("Enter Transaction ID: ");
        String txnId = scanner.nextLine().trim();
        System.out.print("Enter FASTag ID: ");
        String tagId = scanner.nextLine().trim();
        System.out.print("Enter Vehicle Registration: ");
        String reg = scanner.nextLine().trim();
        System.out.print("Enter Disputed Amount (Rs.): ");
        double amt = Double.parseDouble(scanner.nextLine().trim());
        System.out.print("Enter Reason for Dispute: ");
        String reason = scanner.nextLine().trim();

        try {
            // Find transaction timestamp if exists
            LocalDateTime txnTime = transactionRepository.findById(txnId)
                    .map(TollTransaction::timestamp)
                    .orElse(LocalDateTime.now().minusDays(1));

            Dispute d = billingService.raiseDispute(txnId, tagId, reg, amt, txnTime, reason);
            System.out.println("SUCCESS: Dispute filed! Dispute ID: " + d.getDisputeId() + " (Status: RAISED)");
            System.out.println("Awaiting Admin review and decision.");
        } catch (Exception e) {
            System.err.println("REJECTED: " + e.getMessage());
        }
    }

    private void handleReviewDisputes() {
        List<Dispute> disputes = billingService.getAllDisputes();
        if (disputes.isEmpty()) {
            System.out.println("No disputes found.");
            return;
        }

        System.out.println("\n--- Pending & Resolved Disputes ---");
        for (Dispute d : disputes) {
            System.out.printf("Dispute #%s | Txn: %s | Tag: %s | Amount: Rs. %.2f | Status: %s | Reason: %s\n",
                    d.getDisputeId(), d.getTransactionId(), d.getTagId(), d.getDisputedAmount(), d.getStatus(), d.getReason());
        }

        System.out.print("\nEnter Dispute ID to resolve (or press ENTER to skip): ");
        String id = scanner.nextLine().trim();
        if (!id.isEmpty()) {
            System.out.print("Decision: 1. Approve (Refunds to FASTag balance)  2. Reject: ");
            String dec = scanner.nextLine().trim();
            System.out.print("Enter Admin Remarks: ");
            String remarks = scanner.nextLine().trim();

            if ("1".equals(dec)) {
                billingService.approveDispute(id, remarks);
                System.out.println("SUCCESS: Dispute APPROVED! Amount refunded directly to FASTag balance.");
            } else if ("2".equals(dec)) {
                billingService.rejectDispute(id, remarks);
                System.out.println("Dispute REJECTED.");
            }
        }
    }

    private void viewLowBalanceAlerts() {
        List<LowBalanceAlert> alerts = billingService.getAllAlerts();
        System.out.println("\n--- Low Balance Alerts (< Rs. 200 threshold) (" + alerts.size() + ") ---");
        for (LowBalanceAlert a : alerts) {
            System.out.printf("Alert #%s | Tag: %s | Reg: %s | Remaining Bal: Rs. %.2f | Time: %s\n",
                    a.alertId(), a.tagId(), a.vehicleRegNo(), a.balanceAfterDeduction(), a.alertTime().format(TIME_FMT));
        }
    }

    private void viewCashJournal() {
        List<CashPaymentRecord> list = billingService.getAllCashPayments();
        System.out.println("\n--- Cash Toll Payment Journal (" + list.size() + ") ---");
        for (CashPaymentRecord c : list) {
            System.out.printf("Receipt #%s | Plaza: #%d | Lane: #%d | Amount: Rs. %.2f | Operator: %s | Time: %s\n",
                    c.receiptId(), c.plazaId(), c.laneNumber(), c.amount(), c.operatorName(), c.collectedAt().format(TIME_FMT));
        }
    }

    private void viewRecharges() {
        List<RechargeRecord> list = billingService.getAllRecharges();
        System.out.println("\n--- Recharge Records (" + list.size() + ") ---");
        for (RechargeRecord r : list) {
            System.out.printf("Recharge #%s | Tag: %s | Amount: Rs. %.2f | Method: %s | Status: %s | Time: %s\n",
                    r.rechargeId(), r.tagId(), r.amount(), r.method(), r.status(), r.timestamp().format(TIME_FMT));
        }
    }

    // =========================================================================================
    // MODULE 6: EXECUTIVE ANALYTICS & SQL EXERCISES
    // =========================================================================================
    private void handleModule6Analytics() {
        System.out.println("=== [6] EXECUTIVE REPORTS & SQL EXERCISE AGGREGATIONS ===");
        System.out.println(" 1. Plaza Daily Revenue Report");
        System.out.println(" 2. Lane Traffic / Vehicle Count Report");
        System.out.println(" 3. SQL Query 1: Vehicle Type Aggregation (>2 txns, sorted by toll DESC)");
        System.out.println(" 4. SQL Query 2: Plaza Crossing & Failure Audit (>=3 crossings)");
        System.out.println(" 5. SQL Query 3: Multi-Plaza Accounts (>2 distinct plazas, sorted by amount DESC)");
        System.out.print("Select report [1-5]: ");
        String act = scanner.nextLine().trim();

        switch (act) {
            case "1" -> {
                System.out.print("Enter Plaza ID (1, 2, or 3): ");
                int pid = Integer.parseInt(scanner.nextLine().trim());
                double rev = billingService.getDailyRevenue(pid, LocalDate.now());
                System.out.printf("Daily Revenue for Plaza #%d on %s: Rs. %.2f\n", pid, LocalDate.now(), rev);
            }
            case "2" -> {
                System.out.print("Enter Plaza ID (1, 2, or 3): ");
                int pid = Integer.parseInt(scanner.nextLine().trim());
                var counts = billingService.getVehicleCountPerLane(pid, LocalDate.now());
                System.out.println("Vehicle Traffic per Lane for Plaza #" + pid + ":");
                counts.forEach((lane, cnt) -> System.out.printf("  Lane #%d: %d crossings\n", lane, cnt));
            }
            case "3" -> runSQLQuery1();
            case "4" -> runSQLQuery2();
            case "5" -> runSQLQuery3();
            default -> System.out.println("Invalid selection.");
        }
    }

    private void runSQLQuery1() {
        System.out.println("\n----------------- SQL QUERY 1: VEHICLE TYPE SUMMARY -----------------");
        System.out.println("Specification: Total transactions, total toll, and average toll per crossing");
        System.out.println("Filter: Vehicle types with > 2 successful transactions | Sorted by: Total Toll DESC");
        System.out.println("---------------------------------------------------------------------");

        var list = billingService.getQuery1VehicleTypeSummary();
        if (list.isEmpty()) {
            System.out.println("No vehicle types have > 2 successful crossings yet.");
            return;
        }

        System.out.printf("| %-20s | %-12s | %-15s | %-15s |\n",
                "VEHICLE TYPE", "TOTAL TXNS", "TOTAL TOLL", "AVERAGE TOLL");
        System.out.println("+" + "-".repeat(22) + "+" + "-".repeat(14) + "+" + "-".repeat(17) + "+" + "-".repeat(17) + "+");
        for (var r : list) {
            System.out.printf("| %-20s | %-12d | Rs. %-11.2f | Rs. %-11.2f |\n",
                    r.vehicleType().getDisplayName(), r.totalTransactions(), r.totalRevenue(), r.averageToll());
        }
    }

    private void runSQLQuery2() {
        System.out.println("\n----------------- SQL QUERY 2: PLAZA PERFORMANCE AUDIT --------------");
        System.out.println("Specification: Total crossings, total revenue, and failed transactions");
        System.out.println("Filter: Plazas with at least 3 crossings");
        System.out.println("---------------------------------------------------------------------");

        var list = billingService.getQuery2PlazaPerformance();
        if (list.isEmpty()) {
            System.out.println("No plazas have >= 3 crossings yet.");
            return;
        }

        System.out.printf("| %-28s | %-12s | %-15s | %-10s |\n",
                "PLAZA NAME", "CROSSINGS", "TOTAL REVENUE", "FAILED TXNS");
        System.out.println("+" + "-".repeat(30) + "+" + "-".repeat(14) + "+" + "-".repeat(17) + "+" + "-".repeat(12) + "+");
        for (var r : list) {
            System.out.printf("| %-28s | %-12d | Rs. %-11.2f | %-10d |\n",
                    r.plazaName(), r.totalCrossings(), r.totalRevenue(), r.failedCrossings());
        }
    }

    private void runSQLQuery3() {
        System.out.println("\n----------------- SQL QUERY 3: MULTI-PLAZA ACCOUNTS -----------------");
        System.out.println("Specification: Total crossings, total amount deducted, and distinct plazas used");
        System.out.println("Filter: Accounts crossed at > 2 distinct plazas | Sorted by: Total Amount DESC");
        System.out.println("---------------------------------------------------------------------");

        var list = billingService.getQuery3MultiPlazaAccounts();
        if (list.isEmpty()) {
            System.out.println("No FASTag accounts have crossed at > 2 distinct plazas yet.");
            return;
        }

        System.out.printf("| %-14s | %-14s | %-12s | %-15s | %-15s |\n",
                "TAG ID", "VEHICLE REG", "CROSSINGS", "TOTAL DEDUCTED", "DISTINCT PLAZAS");
        System.out.println("+" + "-".repeat(16) + "+" + "-".repeat(16) + "+" + "-".repeat(14) + "+" + "-".repeat(17) + "+" + "-".repeat(17) + "+");
        for (var r : list) {
            System.out.printf("| %-14s | %-14s | %-12d | Rs. %-11.2f | %-15d |\n",
                    r.tagId(), r.vehicleRegNo(), r.totalCrossings(), r.totalDeducted(), r.distinctPlazas());
        }
    }

    // =========================================================================================
    // MODULE 7: AUTOMATED TESTS
    // =========================================================================================
    private void handleRunTestSuites() {
        System.out.println("=== [7] AUTOMATED TEST RUNNER ===");
        System.out.println(" 1. Run Complete 19 End-to-End Integration Tests (All 5 Modules)");
        System.out.println(" 2. Run Module 2 Unit Tests (12 Tests)");
        System.out.print("Select suite [1 or 2]: ");
        String c = scanner.nextLine().trim();

        if ("1".equals(c)) {
            AllModulesTestSuite.main(new String[0]);
        } else if ("2".equals(c)) {
            Module2TestSuite.main(new String[0]);
        } else {
            System.out.println("Invalid selection.");
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 2) + "..";
    }
}
