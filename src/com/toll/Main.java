package com.toll;

import com.toll.cli.MainCLI;
import com.toll.cli.Module2CLI;
import com.toll.cli.Module3CLI;
import com.toll.test.AllModulesTestSuite;
import com.toll.test.Module2TestSuite;
import com.toll.test.Module3TestSuite;

import java.util.Scanner;

/**
 * Main application entry point for the Toll Management System (FASTag).
 * Unifies Master 5-Module Console, Dedicated Module Subsystem CLIs, and All Verification Suites.
 */
public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            System.out.println("\n=========================================================================================");
            System.out.println("     NATIONAL HIGHWAYS AUTHORITY OF INDIA (NHAI) - TOLL MANAGEMENT SYSTEM (FASTag)     ");
            System.out.println("=========================================================================================");
            System.out.println(" Select Console Interface:");
            System.out.println("  [1] Master 5-Module Integrated Toll Management Console (Plaza, Fastag, Toll, Passes, Billing)");
            System.out.println("  [2] Module 2 Dedicated Subsystem (Vehicle & FASTag Accounts)");
            System.out.println("  [3] Module 3 Dedicated Subsystem (Toll Processing Engine & Barriers)");
            System.out.println("  [4] Run All Automated Verification Suites (Module 2, Module 3 & Full System)");
            System.out.println("  [5] Exit Application");
            System.out.println("=========================================================================================");
            System.out.print("Enter choice [1-5]: ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> new MainCLI().start();
                case "2" -> new Module2CLI().start();
                case "3" -> new Module3CLI().start();
                case "4" -> {
                    System.out.println("\n>>> EXECUTING MODULE 2 TEST SUITE <<<");
                    Module2TestSuite.main(new String[0]);
                    System.out.println("\n>>> EXECUTING MODULE 3 TEST SUITE <<<");
                    Module3TestSuite.main(new String[0]);
                    System.out.println("\n>>> EXECUTING COMPLETE 5-MODULE INTEGRATION TEST SUITE <<<");
                    AllModulesTestSuite.main(new String[0]);
                }
                case "5" -> {
                    System.out.println("Exiting Toll Management System. Safe Travels!");
                    running = false;
                }
                default -> System.out.println("Invalid selection. Please choose between 1 and 5.");
            }
        }
    }
}
