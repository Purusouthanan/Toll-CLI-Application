package com.toll.cli;

import com.toll.common.AccountStatus;
import com.toll.common.DataSeeder;
import com.toll.common.VehicleType;
import com.toll.module2.account.exception.FASTagAccountException;
import com.toll.module2.account.model.FASTagAccount;
import com.toll.module2.account.model.Vehicle;
import com.toll.module2.account.repository.InMemoryFASTagAccountRepository;
import com.toll.module2.account.repository.InMemoryVehicleRepository;
import com.toll.module2.account.service.AccountServiceImpl;
import com.toll.module2.account.service.DebitResult;
import com.toll.module2.account.service.IAccountService;
import com.toll.module2.account.service.RechargeResult;
import com.toll.test.Module2TestSuite;

import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/**
 * Interactive Console CLI for Module 2: Vehicle & FASTag Accounts.
 * Features Indian Highway theme, clear feedback, and comprehensive test workflows.
 */
public class Module2CLI {
    private final IAccountService accountService;
    private final Scanner scanner;

    public Module2CLI() {
        this.accountService = new AccountServiceImpl(
                new InMemoryVehicleRepository(),
                new InMemoryFASTagAccountRepository()
        );
        this.scanner = new Scanner(System.in);
        // Pre-seed realistic Indian toll test data
        DataSeeder.seedModule2Data(accountService);
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
                case "1" -> handleRegisterVehicle();
                case "2" -> handleIssueFASTag();
                case "3" -> handleViewAllAccounts();
                case "4" -> handleSearchAccount();
                case "5" -> handleRecharge();
                case "6" -> handleSimulateTollDebit();
                case "7" -> handleAdminBlacklist();
                case "8" -> handleRunTestSuite();
                case "9" -> {
                    System.out.println("Exiting Toll Management System (FASTag). Safe Travels!");
                    running = false;
                }
                default -> System.out.println("Invalid option. Please choose between 1 and 9.");
            }
            if (running) {
                System.out.println("\nPress [ENTER] to return to main menu...");
                scanner.nextLine();
            }
        }
    }

    private void printBanner() {
        System.out.println("=========================================================================================");
        System.out.println("     NATIONAL HIGHWAYS AUTHORITY OF INDIA (NHAI) - TOLL MANAGEMENT SYSTEM (FASTag)     ");
        System.out.println("                      MODULE 2: VEHICLE & FASTAG ACCOUNTS ENGINE                        ");
        System.out.println("=========================================================================================");
        System.out.println(" Initialized in-memory data store with Indian highway test vehicles & FASTag accounts.");
    }

    private void printMainMenu() {
        System.out.println("\n----------------------------- MAIN MENU (MODULE 2) -----------------------------");
        System.out.println(" [1] Register New Vehicle (Car, Van, Bus, Truck, Multi-Axle)");
        System.out.println(" [2] Issue & Link FASTag Account (Enforces strict 1:1 association)");
        System.out.println(" [3] View All Vehicles & Linked FASTags (Tabular Overview)");
        System.out.println(" [4] Search Account / Vehicle Details (By Tag ID or Registration No)");
        System.out.println(" [5] Recharge FASTag Account (Min Rs. 100, Max Rs. 10,000, Auto-Unblock Check)");
        System.out.println(" [6] Simulate Toll Deduction (Toll Engine Call, Auto-Block < Rs. 100)");
        System.out.println(" [7] Admin Tag Blacklist / Unblacklist Management");
        System.out.println(" [8] Run Automated Module 2 Verification Test Suite (12 Tests)");
        System.out.println(" [9] Exit");
        System.out.println("--------------------------------------------------------------------------------");
    }

    private void handleRegisterVehicle() {
        System.out.println("--- [1] Register New Vehicle ---");
        System.out.println("Select Vehicle Type:");
        System.out.println(" 1. Car / Jeep / SUV (LMV)");
        System.out.println(" 2. Van / Mini-Bus (LCV)");
        System.out.println(" 3. Bus (Heavy Commercial)");
        System.out.println(" 4. Truck / Lorry (2-Axle)");
        System.out.println(" 5. Multi-Axle Vehicle (MAV - 3+ Axles)");
        System.out.print("Enter choice [1-5]: ");
        String typeChoice = scanner.nextLine().trim();

        VehicleType type = switch (typeChoice) {
            case "1" -> VehicleType.CAR;
            case "2" -> VehicleType.VAN;
            case "3" -> VehicleType.BUS;
            case "4" -> VehicleType.TRUCK;
            case "5" -> VehicleType.MULTI_AXLE;
            default -> null;
        };

        if (type == null) {
            System.out.println("Invalid vehicle type selected. Aborting registration.");
            return;
        }

        System.out.print("Enter Registration Number (e.g. MH-12-AB-1234): ");
        String regNo = scanner.nextLine().trim();

        System.out.print("Enter Owner Full Name: ");
        String ownerName = scanner.nextLine().trim();

        System.out.print("Enter Contact Phone Number: ");
        String phone = scanner.nextLine().trim();

        try {
            Vehicle vehicle;
            if (type == VehicleType.MULTI_AXLE) {
                System.out.print("Enter number of axles (min 3, e.g. 4): ");
                int axles = Integer.parseInt(scanner.nextLine().trim());
                vehicle = accountService.registerMultiAxleVehicle(regNo, ownerName, phone, axles);
            } else {
                vehicle = accountService.registerVehicle(type, regNo, ownerName, phone);
            }
            System.out.println("\nSUCCESS: Vehicle registered successfully!");
            System.out.println("Details: " + vehicle);
            System.out.println("Category: " + vehicle.getCategoryDescription());
        } catch (FASTagAccountException | IllegalArgumentException e) {
            System.err.println("\nREGISTRATION FAILED: " + e.getMessage());
        }
    }

    private void handleIssueFASTag() {
        System.out.println("--- [2] Issue & Link FASTag Account ---");
        System.out.print("Enter Registered Vehicle Registration Number (e.g. MH-12-DE-1433): ");
        String regNo = scanner.nextLine().trim();

        System.out.print("Enter New Tag ID (e.g. IND-TAG-8801): ");
        String tagId = scanner.nextLine().trim();

        System.out.print("Enter Initial Deposit Balance (Rs. 100 - Rs. 50,000): ");
        try {
            double deposit = Double.parseDouble(scanner.nextLine().trim());
            FASTagAccount account = accountService.issueFASTag(regNo, tagId, deposit);
            System.out.println("\nSUCCESS: FASTag issued and linked 1:1 to vehicle!");
            System.out.println("Tag ID: " + account.getTagId());
            System.out.println("Linked Vehicle: " + account.getVehicleRegistrationNumber());
            System.out.printf("Initial Balance: Rs. %.2f\n", account.getBalance());
            System.out.println("Account Status: " + account.getStatus());
        } catch (NumberFormatException e) {
            System.err.println("Invalid number format for deposit.");
        } catch (FASTagAccountException | IllegalArgumentException e) {
            System.err.println("\nISSUANCE FAILED: " + e.getMessage());
        }
    }

    private void handleViewAllAccounts() {
        System.out.println("--- [3] All Registered Vehicles & FASTag Accounts ---");
        List<Vehicle> vehicles = accountService.getAllVehicles();

        if (vehicles.isEmpty()) {
            System.out.println("No vehicles registered in the system.");
            return;
        }

        System.out.println("+" + "-".repeat(16) + "+" + "-".repeat(12) + "+" + "-".repeat(20) + "+" + "-".repeat(16) + "+" + "-".repeat(12) + "+" + "-".repeat(10) + "+" + "-".repeat(14) + "+");
        System.out.printf("| %-14s | %-10s | %-18s | %-14s | %-10s | %-8s | %-12s |\n",
                "REG NUMBER", "TYPE", "OWNER", "TAG ID", "BALANCE", "STATUS", "BLACKLISTED");
        System.out.println("+" + "-".repeat(16) + "+" + "-".repeat(12) + "+" + "-".repeat(20) + "+" + "-".repeat(16) + "+" + "-".repeat(12) + "+" + "-".repeat(10) + "+" + "-".repeat(14) + "+");

        for (Vehicle v : vehicles) {
            Optional<FASTagAccount> accOpt = accountService.findAccountByVehicleRegistration(v.getRegistrationNumber());
            String tagId = accOpt.map(FASTagAccount::getTagId).orElse("(No Tag)");
            String balance = accOpt.map(a -> String.format("Rs.%.2f", a.getBalance())).orElse("N/A");
            String status = accOpt.map(a -> a.getStatus().name()).orElse("N/A");
            String blacklisted = accOpt.map(a -> a.isBlacklisted() ? "YES" : "NO").orElse("NO");

            System.out.printf("| %-14s | %-10s | %-18s | %-14s | %-10s | %-8s | %-12s |\n",
                    v.getRegistrationNumber(),
                    v.getVehicleType().name(),
                    truncate(v.getOwnerName(), 18),
                    tagId,
                    balance,
                    status,
                    blacklisted);
        }
        System.out.println("+" + "-".repeat(16) + "+" + "-".repeat(12) + "+" + "-".repeat(20) + "+" + "-".repeat(16) + "+" + "-".repeat(12) + "+" + "-".repeat(10) + "+" + "-".repeat(14) + "+");
    }

    private void handleSearchAccount() {
        System.out.println("--- [4] Search Account / Vehicle Details ---");
        System.out.print("Enter FASTag ID or Vehicle Registration Number: ");
        String query = scanner.nextLine().trim();

        Optional<FASTagAccount> byTag = accountService.findAccountByTagId(query);
        Optional<FASTagAccount> byReg = byTag.isPresent() ? byTag : accountService.findAccountByVehicleRegistration(query);

        if (byReg.isPresent()) {
            FASTagAccount acc = byReg.get();
            System.out.println("\nFASTag Account Found:");
            System.out.println(" Tag ID:              " + acc.getTagId());
            System.out.println(" Vehicle Reg No:      " + acc.getVehicleRegistrationNumber());
            System.out.printf(" Current Balance:     Rs. %.2f\n", acc.getBalance());
            System.out.println(" Account Status:      " + acc.getStatus() + " (" + acc.getStatus().getDescription() + ")");
            System.out.println(" Blacklisted:         " + (acc.isBlacklisted() ? "YES - " + acc.getBlacklistReason() : "NO"));
            System.out.println(" Created At:          " + acc.getCreatedAt());
            System.out.println(" Last Updated:        " + acc.getUpdatedAt());

            accountService.findVehicleByRegistration(acc.getVehicleRegistrationNumber()).ifPresent(v -> {
                System.out.println("\nLinked Vehicle Details:");
                System.out.println(" Owner Name:          " + v.getOwnerName());
                System.out.println(" Phone Number:        " + v.getPhoneNumber());
                System.out.println(" Vehicle Type:        " + v.getVehicleType().getDisplayName());
                System.out.println(" Category Info:       " + v.getCategoryDescription());
                System.out.println(" Axle Count:          " + v.getAxleCount());
            });
        } else {
            // Check if vehicle exists without tag
            Optional<Vehicle> vOpt = accountService.findVehicleByRegistration(query);
            if (vOpt.isPresent()) {
                Vehicle v = vOpt.get();
                System.out.println("\nVehicle Found (No FASTag linked):");
                System.out.println(" Details: " + v);
                System.out.println(" Note: Vehicle does not have an active FASTag account.");
            } else {
                System.out.println("\nNo record found for query: " + query);
            }
        }
    }

    private void handleRecharge() {
        System.out.println("--- [5] Recharge FASTag Account ---");
        System.out.print("Enter FASTag ID (e.g. IND-TAG-1002): ");
        String tagId = scanner.nextLine().trim();

        System.out.print("Enter Recharge Amount (Rs. 100 - Rs. 10,000): ");
        try {
            double amount = Double.parseDouble(scanner.nextLine().trim());
            RechargeResult result = accountService.rechargeAccount(tagId, amount);

            if (result.isSuccessful()) {
                System.out.println("\nRECHARGE SUCCESSFUL!");
                System.out.println(result.getMessage());
                if (result.isAutoUnblocked()) {
                    System.out.println("STATE TRANSITION: Account balance reached >= Rs. 100. Status automatically restored to ACTIVE!");
                }
            } else {
                System.err.println("\nRECHARGE REJECTED: " + result.getMessage());
            }
        } catch (NumberFormatException e) {
            System.err.println("Invalid numeric amount.");
        }
    }

    private void handleSimulateTollDebit() {
        System.out.println("--- [6] Simulate Toll Deduction (Toll Engine Pipeline) ---");
        System.out.print("Enter FASTag ID (e.g. IND-TAG-1001): ");
        String tagId = scanner.nextLine().trim();

        System.out.print("Enter Toll Fee to Deduct (e.g. 80.00): ");
        try {
            double toll = Double.parseDouble(scanner.nextLine().trim());
            DebitResult result = accountService.deductToll(tagId, toll);

            if (result.isSuccessful()) {
                System.out.println("\nTOLL DEDUCTION SUCCESSFUL (Barrier Opens):");
                System.out.println(" " + result.getMessage());
                if (result.isAutoBlocked()) {
                    System.out.println(" ATTENTION: Remaining balance is below Rs. 100. Account is now BLOCKED!");
                }
                if (result.isLowBalanceAlertTriggered()) {
                    System.out.println(" NOTIFICATION: Low balance alert (< Rs. 200) triggered for Module 5 Notification Engine.");
                }
            } else {
                System.err.println("\nTOLL DEDUCTION FAILED (Barrier Remains Closed):");
                System.err.println(" Reason: " + result.getMessage());
            }
        } catch (NumberFormatException e) {
            System.err.println("Invalid numeric amount.");
        }
    }

    private void handleAdminBlacklist() {
        System.out.println("--- [7] Admin Tag Blacklist Management ---");
        System.out.print("Enter FASTag ID: ");
        String tagId = scanner.nextLine().trim();

        Optional<FASTagAccount> accOpt = accountService.findAccountByTagId(tagId);
        if (accOpt.isEmpty()) {
            System.err.println("Tag not found: " + tagId);
            return;
        }

        FASTagAccount acc = accOpt.get();
        System.out.println("Current status for " + tagId + ": " + (acc.isBlacklisted() ? "BLACKLISTED" : "CLEAR"));
        System.out.println("1. Blacklist Tag (Flag as stolen/fraudulent)");
        System.out.println("2. Unblacklist Tag (Clear flag)");
        System.out.print("Choose action [1 or 2]: ");
        String act = scanner.nextLine().trim();

        if ("1".equals(act)) {
            System.out.print("Enter Blacklist Reason: ");
            String reason = scanner.nextLine().trim();
            accountService.blacklistTag(tagId, reason);
            System.out.println("Tag " + tagId + " has been BLACKLISTED. It will be refused at all toll plazas.");
        } else if ("2".equals(act)) {
            accountService.unblacklistTag(tagId);
            System.out.println("Tag " + tagId + " blacklist flag cleared. Operational status restored.");
        } else {
            System.out.println("Invalid action.");
        }
    }

    private void handleRunTestSuite() {
        System.out.println("--- [8] Running Automated Test Suite ---");
        Module2TestSuite.main(new String[0]);
    }

    private static String truncate(String str, int maxLen) {
        if (str == null) return "";
        return str.length() <= maxLen ? str : str.substring(0, maxLen - 2) + "..";
    }
}
