package com.toll;

import com.toll.cli.Module2CLI;
import com.toll.cli.Module3CLI;
import com.toll.test.Module2TestSuite;
import com.toll.test.Module3TestSuite;

import java.util.Scanner;

/**
 * Main application entry point for the Toll Management System (FASTag).
 * Provides access to Module 2 (Vehicle & Accounts) and Module 3 (Toll Processing Engine).
 */
public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            System.out.println("\n=========================================================================================");
            System.out.println("     NATIONAL HIGHWAYS AUTHORITY OF INDIA (NHAI) - TOLL MANAGEMENT SYSTEM (FASTag)     ");
            System.out.println("=========================================================================================");
            System.out.println(" Select Subsystem Console:");
            System.out.println("  [1] Module 2: Vehicle & FASTag Accounts Console");
            System.out.println("  [2] Module 3: Toll Processing Engine Console (Crossing, Barriers, AVC Mismatch)");
            System.out.println("  [3] Run All Automated Verification Test Suites (Module 2 & Module 3)");
            System.out.println("  [4] Exit");
            System.out.println("=========================================================================================");
            System.out.print("Enter choice [1-4]: ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> new Module2CLI().start();
                case "2" -> new Module3CLI().start();
                case "3" -> {
                    System.out.println("\n>>> EXECUTING MODULE 2 TEST SUITE <<<");
                    Module2TestSuite.main(new String[0]);
                    System.out.println("\n>>> EXECUTING MODULE 3 TEST SUITE <<<");
                    Module3TestSuite.main(new String[0]);
                }
                case "4" -> {
                    System.out.println("Exiting Toll Management System. Safe Travels!");
                    running = false;
                }
                default -> System.out.println("Invalid selection. Please choose between 1 and 4.");
            }
        }
    }
}
