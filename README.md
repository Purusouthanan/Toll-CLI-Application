# National Highway Authority of India (NHAI) - Toll Management System (FASTag)
## Low-Level Design (LLD) & Console CLI Application

[![Java Version](https://img.shields.io/badge/Java-21%20%7C%2023-orange.svg)](https://www.oracle.com/java/)
[![Architecture](https://img.shields.io/badge/Design-Domain--Driven%20(DDD)-blue.svg)]()
[![OOP Tenets](https://img.shields.io/badge/OOP-Inheritance%20%7C%20Polymorphism%20%7C%20Encapsulation%20%7C%20Abstraction-green.svg)]()
[![Tests](https://img.shields.io/badge/Automated%20Tests-12%2F12%20Passing-brightgreen.svg)]()

---

## 1. Project Overview

This repository contains the object-oriented low-level design (LLD) and console application for an Indian Highway **FASTag Toll Management System**, built per the official NHAI and MoRTH technical problem specification.

The project is architected for a **5-member engineering team**, divided across modular boundaries with strict encapsulation, abstract service contracts, and in-memory thread-safe collections.

---

## 2. 5-Member Team Division & Interface Contracts

| Member | Role | Module | Assigned Package | Key Service Interface |
| :--- | :--- | :--- | :--- | :--- |
| **Member 1** | Plaza & Infrastructure | **Module 1**: Toll Plaza & Lane Setup | `com.toll.module1.plaza` | `IPlazaService` |
| **Member 2 (Repo Owner)** | Vehicle & Accounts Lead | **Module 2**: Vehicle & FASTag Accounts | `com.toll.module2.account` | `IAccountService` |
| **Member 3** | Processing & Hardware Engine | **Module 3**: Toll Processing Engine | `com.toll.module3.tollengine` | `ITollProcessingService` |
| **Member 4** | Pricing & Subscriptions | **Module 4**: Rates, Discounts & Passes | `com.toll.module4.pricing` | `IPricingService` |
| **Member 5** | Billing & Reporting | **Module 5**: Payments, Disputes & Reports | `com.toll.module5.billing` | `IBillingReportingService` |

---

## 3. Module 2: Vehicle & FASTag Accounts (Completed Deliverables)

As the **Repository Owner and Module 2 Lead**, this module has been fully implemented, strictly tested, and verified.

### Core Business Rules Implemented:
1. **Vehicle Registry**:
   - Unique Indian registration numbers (e.g., `MH-12-DE-1433`, `KA-01-MJ-5566`).
   - Extended vehicle hierarchy: `Vehicle` base class extended by `Car`, `Van`, `Bus`, `Truck`, and `MultiAxleVehicle`.
   - Polymorphic methods: `getAxleCount()`, `getCategoryDescription()`.
2. **1:1 FASTag Association**:
   - Exactly one active FASTag linked per vehicle.
   - Secondary index enforcement prevents duplicate tag allocations or multi-tagging.
3. **State Transitions & Balances**:
   - Statuses: `ACTIVE`, `BLOCKED`, `CLOSED`.
   - Balance is **never negative**.
   - Maximum account balance cap: **₹50,000.00**.
   - Recharge bounds: Min **₹100.00**, Max **₹10,000.00**.
   - **Auto-Block**: Automatically blocks (`BLOCKED`) when balance drops below **₹100.00**.
   - **Auto-Unblock**: Automatically unblocks (`ACTIVE`) when recharge takes balance to **$\ge$ ₹100.00**.
4. **Admin Blacklist Engine**:
   - Admin can flag stolen/fraudulent tags. Blacklisted tags are refused at every toll plaza.
5. **Cross-Module Triggers**:
   - Low balance alert trigger when remaining balance drops below **₹200.00** after toll deduction (for Member 5's notification queue).
   - Dispute refund support directly to FASTag balance.

---

## 4. Package Structure

```
src/
└── com/
    └── toll/
        ├── Main.java                          <-- Console Application Entry Point
        ├── cli/
        │   └── Module2CLI.java                <-- Interactive CLI for Module 2
        ├── common/
        │   ├── AccountStatus.java             <-- ACTIVE, BLOCKED, CLOSED
        │   ├── VehicleType.java               <-- CAR, VAN, BUS, TRUCK, MULTI_AXLE
        │   └── DataSeeder.java                <-- Initial test data generator
        ├── module1/plaza/
        │   └── IPlazaService.java             <-- Member 1 Interface Contract
        ├── module2/account/                   <-- MEMBER 2 (COMPLETED)
        │   ├── exception/                     <-- Custom business exceptions
        │   ├── model/                         <-- Vehicle hierarchy & FASTagAccount
        │   ├── repository/                    <-- In-memory thread-safe repositories
        │   └── service/                       <-- IAccountService & AccountServiceImpl
        ├── module3/tollengine/
        │   └── ITollProcessingService.java    <-- Member 3 Interface Contract
        ├── module4/pricing/
        │   └── IPricingService.java           <-- Member 4 Interface Contract
        ├── module5/billing/
        │   └── IBillingReportingService.java  <-- Member 5 Interface Contract
        └── test/
            └── Module2TestSuite.java          <-- 12 Automated Verification Tests
```

---

## 5. Quick Start: Build, Run & Test

No external Maven/Gradle installation required. Pure Java SE 21/23.

### Using Batch Files (Windows Command Prompt)
```cmd
:: 1. Compile the project
build.bat

:: 2. Run the automated 12-test suite
test.bat

:: 3. Launch interactive CLI application
run.bat
```

### Direct Terminal Commands (PowerShell / Bash)
```powershell
# Compile
javac -d bin (Get-ChildItem -Path src -Filter *.java -Recurse).FullName

# Run Test Suite
java -cp bin com.toll.test.Module2TestSuite

# Run Interactive CLI
java -cp bin com.toll.Main
```

---

## 6. Verification Test Results

```
===============================================================================
        RUNNING MODULE 2 (VEHICLE & FASTAG ACCOUNTS) TEST SUITE                
===============================================================================
 [PASS] 1. Unique Vehicle Registration Rule
 [PASS] 2. Strict 1:1 Tag-to-Vehicle Association
 [PASS] 3. Auto-Block State Machine (Balance < Rs. 100)
 [PASS] 4. Minimum Recharge Limit (Min Rs. 100)
 [PASS] 5. Maximum Recharge Limit (Max Rs. 10,000)
 [PASS] 6. Maximum Balance Cap (Max Rs. 50,000)
 [PASS] 7. Auto-Unblock State Machine (Recharge to >= Rs. 100)
 [PASS] 8. Non-Negative Balance Constraint
 [PASS] 9. Admin Blacklist & Plaza Refusal
 [PASS] 10. Dispute Refund Credit & State Unblock
 [PASS] 11. OOP Inheritance & Polymorphism (Vehicle Hierarchy)
 [PASS] 12. Low Balance Alert Flagging (< Rs. 200)
===============================================================================
 TEST SUMMARY: Total: 12 | Passed: 12 | Failed: 0
===============================================================================
ALL MODULE 2 TESTS PASSED PERFECTLY!
```
