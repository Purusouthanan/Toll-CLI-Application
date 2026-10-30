# National Highway Authority of India (NHAI) - Toll Management System (FASTag)
## Low-Level Design (LLD) & Complete 5-Module Interactive Console Application

[![Java Version](https://img.shields.io/badge/Java-21%20%7C%2023-orange.svg)](https://www.oracle.com/java/)
[![Architecture](https://img.shields.io/badge/Design-Domain--Driven%20(DDD)-blue.svg)]()
[![OOP Tenets](https://img.shields.io/badge/OOP-Inheritance%20%7C%20Polymorphism%20%7C%20Encapsulation%20%7C%20Abstraction-green.svg)]()
[![Integration Tests](https://img.shields.io/badge/Integration%20Tests-19%2F19%20Passing-brightgreen.svg)]()
[![Module 2 Tests](https://img.shields.io/badge/Module%202%20Tests-12%2F12%20Passing-brightgreen.svg)]()

---

## 1. Project Overview

The **Toll Management System (FASTag)** is a production-grade, object-oriented highway toll collection application designed in accordance with **NHAI (National Highways Authority of India)** and **MoRTH** operational guidelines.

It provides a unified, thread-safe, in-memory domain model and an interactive console CLI simulating the entire toll plaza crossing lifecycle, dynamic rate calculations, return-trip discounts, FASTag account balances, cash fallbacks, disputes, and analytics.

---

## 2. 5-Module Architecture & Scope Breakdown

The system cleanly separates responsibilities across **5 autonomous domain modules**, demonstrating high cohesion and loose coupling:

```
+---------------------------------------------------------------------------------------------------+
|                                MASTER TOLL MANAGEMENT SYSTEM (FASTag)                             |
+---------------------------------------------------------------------------------------------------+
|  [Module 1: Plaza & Lanes]       [Module 2: Vehicle & FASTag]       [Module 3: Toll Engine]       |
|  - Toll Plaza lifecycle          - Vehicle Class Hierarchy          - RFID Crossing Pipeline      |
|  - Multi-modal Lanes (>= 2)      - 1:1 Tag Association              - 2-Min Duplicate Cooldown    |
|  - Effective Rate Engine         - Auto-Block (< Rs. 100)           - Optical AVC Mismatch Engine |
|  - Lane Busy Safety Lock         - Auto-Unblock (>= Rs. 100)        - Cash Fallback Router (2x/1x)|
|                                  - Admin Blacklist Refusal          - Emergency Lane (Rs. 0)      |
+---------------------------------------------------------------------------------------------------+
|  [Module 4: Rates, Discounts & Passes]             [Module 5: Payments, Disputes & Reports]       |
|  - 24-Hour Return Trip Engine (50% discount)       - Polymorphic PaymentMethod Strategy           |
|  - Monthly Pass Manager (Rs. 0 toll crossings)     - UPI / Card / Net Banking Recharges           |
|  - Nearest Rs. 5 Rounding Algorithm                - 7-Day Dispute & Auto-Refund to Balance       |
|  - Single Active Pass per Plaza Constraint         - Low Balance Alert (< Rs. 200)               |
|                                                    - Daily Revenue, Traffic & SQL Queries 1, 2, 3 |
+---------------------------------------------------------------------------------------------------+
```

---

## 3. Demonstration of Object-Oriented Programming (OOP) Tenets

Per the project specification, the codebase strictly models core OOP fundamentals:

1. **Encapsulation**:
   - Internal state fields in `FASTagAccount`, `TollPlaza`, `Lane`, `Vehicle`, and `Dispute` are strictly `private`.
   - State mutations occur only via validated domain methods (e.g., `debit()`, `recharge()`, `close()`, `open()`, `approve()`).
2. **Inheritance**:
   - **Vehicle Hierarchy**: Abstract base `Vehicle` extended by `Car`, `Van`, `Bus`, `Truck`, and `MultiAxleVehicle`.
   - **Lane Hierarchy**: Abstract base `Lane` extended by `FASTagLane`, `CashLane`, and `EmergencyLane`.
3. **Polymorphism**:
   - **Payment Processing**: `PaymentMethod` strategy interface polymorphically implemented by `FASTagPaymentMethod` (debiting FASTag balance) and `CashPaymentMethod` (recording lane operator).
   - **Vehicle Polymorphism**: Abstract methods `getAxleCount()` and `getCategoryDescription()` implemented uniquely across vehicle classes.
4. **Abstraction**:
   - Clean service interfaces (`IPlazaService`, `IAccountService`, `ITollProcessingService`, `IPricingService`, `IBillingReportingService`) conceal in-memory repository implementations (`ConcurrentHashMap`, `CopyOnWriteArrayList`).

---

## 4. Package Structure

```
src/
└── com/
    └── toll/
        ├── Main.java                               <-- Master CLI Entry Point
        ├── cli/
        │   ├── MainCLI.java                        <-- Complete 5-Module Interactive Console UI
        │   └── Module2CLI.java                     <-- Standalone Module 2 CLI
        ├── common/
        │   ├── AccountStatus.java                  <-- ACTIVE, BLOCKED, CLOSED
        │   ├── DisputeStatus.java                  <-- RAISED, APPROVED, REJECTED
        │   ├── LaneStatus.java & LaneType.java     <-- FASTAG, CASH, EMERGENCY
        │   ├── PaymentType.java                    <-- FASTAG, CASH, EXEMPT
        │   ├── PlazaStatus.java                    <-- INACTIVE, ACTIVE
        │   ├── RechargeMethod.java                 <-- UPI, CARD, NET_BANKING
        │   ├── RechargeStatus.java                 <-- PAID, FAILED, REFUNDED
        │   ├── TransactionStatus.java              <-- SUCCESS, FAILED, PENDING
        │   ├── TripType.java                       <-- SINGLE, RETURN, PASS, EMERGENCY
        │   ├── VehicleType.java                    <-- CAR, VAN, BUS, TRUCK, MULTI_AXLE
        │   └── DataSeeder.java                     <-- Pre-seeds Plazas, Lanes, Vehicles & FASTags
        ├── module1/plaza/                          <-- MODULE 1: PLAZA & LANES
        │   ├── model/ (Lane, FASTagLane, CashLane, EmergencyLane, TollRate, TollPlaza)
        │   ├── repository/ (IPlazaRepository, InMemoryPlazaRepository)
        │   └── service/ (IPlazaService, PlazaServiceImpl)
        ├── module2/account/                        <-- MODULE 2: VEHICLE & FASTAG ACCOUNTS
        │   ├── exception/ (Custom domain exceptions)
        │   ├── model/ (Vehicle, Car, Van, Bus, Truck, MultiAxleVehicle, FASTagAccount, VehicleFactory)
        │   ├── repository/ (IVehicleRepository, IFASTagAccountRepository)
        │   └── service/ (IAccountService, AccountServiceImpl, DebitResult, RechargeResult)
        ├── module3/tollengine/                     <-- MODULE 3: TOLL PROCESSING ENGINE
        │   ├── model/ (TollTransaction)
        │   ├── repository/ (ITransactionRepository, InMemoryTransactionRepository)
        │   └── service/ (ITollProcessingService, TollProcessingServiceImpl)
        ├── module4/pricing/                        <-- MODULE 4: RATES, DISCOUNTS & PASSES
        │   ├── model/ (MonthlyPass)
        │   ├── repository/ (IPassRepository, InMemoryPassRepository)
        │   └── service/ (IPricingService, PricingServiceImpl, PricingResult)
        ├── module5/billing/                        <-- MODULE 5: PAYMENTS, DISPUTES & REPORTS
        │   ├── model/ (RechargeRecord, CashPaymentRecord, Dispute, LowBalanceAlert)
        │   ├── payment/ (PaymentMethod, FASTagPaymentMethod, CashPaymentMethod, PaymentResult)
        │   └── service/ (IBillingReportingService, BillingReportingServiceImpl)
        └── test/
            ├── AllModulesTestSuite.java            <-- 19 Integrated System Tests
            └── Module2TestSuite.java               <-- 12 Dedicated Module 2 Unit Tests
```

---

## 5. Complete Business Rules Coverage

| Module | Business Rule | Implementation Location |
| :--- | :--- | :--- |
| **Module 1** | Only Admin can create plazas & lanes ($\ge 2$ lanes required) | `TollPlaza.java`, `PlazaServiceImpl.java` |
| **Module 1** | Plaza cannot open without rates for all 5 vehicle types | `TollPlaza.open()`, `hasRatesForEveryVehicleType()` |
| **Module 1** | Lane safety lock: Lane cannot close if vehicle is processing | `Lane.close()`, `isProcessingVehicle()` |
| **Module 1** | Rate changes apply strictly on/after effective date | `TollRate.isEffectiveOn()`, `TollPlaza.getActiveRate()` |
| **Module 2** | Unique registration numbers, 1:1 tag-to-vehicle link | `InMemoryFASTagAccountRepository.java` |
| **Module 2** | Recharge bounds: Min ₹100, Max ₹10,000, Max Balance ₹50,000 | `FASTagAccount.recharge()` |
| **Module 2** | Balance can never be negative | `FASTagAccount.debit()` |
| **Module 2** | Auto-block when balance < ₹100 | `FASTagAccount.debit()` |
| **Module 2** | Auto-unblock when recharge takes balance $\ge$ ₹100 | `FASTagAccount.recharge()`, `refund()` |
| **Module 2** | Admin blacklisting refuses tag at every plaza | `FASTagAccount.blacklist()`, `TollProcessingServiceImpl` |
| **Module 3** | Barrier opens only after successful toll deduction | `TollProcessingServiceImpl.processFASTagCrossing()` |
| **Module 3** | Optical AVC mismatch: Charges higher toll & flags review | `PricingServiceImpl.calculateFare()`, `TollTransaction` |
| **Module 3** | Vehicle without active tag diverted to Cash lane (2x penalty) | `TollProcessingServiceImpl.processCashCrossing()` |
| **Module 3** | Insufficient balance diverted to Cash lane (Normal toll) | `TollProcessingServiceImpl.processFASTagCrossing()` |
| **Module 3** | 2-minute anti-double-billing cooldown protection | `TollProcessingServiceImpl.processFASTagCrossing()` |
| **Module 3** | Emergency vehicles pay ₹0 in Emergency lane | `TollProcessingServiceImpl.processEmergencyCrossing()` |
| **Module 4** | 24-hour return trip: 50% discount on Single rate | `PricingServiceImpl.calculateFare()` |
| **Module 4** | Monthly pass: ₹0 toll crossing at designated plaza | `MonthlyPass.isValidOn()`, `PricingServiceImpl` |
| **Module 4** | All computed tolls rounded to nearest ₹5 | `PricingServiceImpl.roundToNearestFive()` |
| **Module 5** | Recharges via UPI, Card, Net Banking recorded | `BillingReportingServiceImpl.processRecharge()` |
| **Module 5** | Cash toll recorded with collecting operator's name | `CashPaymentMethod`, `CashPaymentRecord` |
| **Module 5** | 7-day dispute limit; approved disputes refund to FASTag | `BillingReportingServiceImpl.raiseDispute()`, `approveDispute()` |
| **Module 5** | Low balance alert triggered when balance < ₹200 | `BillingReportingServiceImpl.triggerLowBalanceAlert()` |
| **Module 5** | Daily plaza revenue & lane vehicle count reports | `BillingReportingServiceImpl.getDailyRevenue()` |
| **Module 5** | SQL Queries 1, 2, 3 in-memory aggregations | `BillingReportingServiceImpl.getQuery1/2/3...` |

---

## 6. How to Build, Run & Test

Pure Java SE 21 / 23. No external dependencies or build tools required.

### Quick Start with Batch Scripts (Windows Command Prompt)
```cmd
:: 1. Compile the entire application
build.bat

:: 2. Run the 19 integration tests
test.bat

:: 3. Launch the master interactive CLI
run.bat
```

### Direct Terminal Commands (PowerShell)
```powershell
# Compile
javac -d bin (Get-ChildItem -Path src -Filter *.java -Recurse).FullName

# Run Master Interactive CLI
java -Dfile.encoding=UTF-8 -cp bin com.toll.Main

# Run Integration Test Suite
java -cp bin com.toll.test.AllModulesTestSuite
```

---

## 7. Automated Test Verification Results

All 19 end-to-end integration tests execute and pass with 100% success rate:

```
=========================================================================================
        COMPREHENSIVE END-TO-END VERIFICATION SUITE (ALL 5 MODULES INTEGRATED)           
=========================================================================================
 [PASS] M1-1: Plaza requires >= 2 lanes to open
 [PASS] M1-2: Plaza cannot open without rates for ALL 5 vehicle types
 [PASS] M1-3: Lane close rejected when vehicle is being processed
 [PASS] M2-1: Unique vehicle registration and 1:1 tag link
 [PASS] M2-2: Auto-block (< Rs. 100) and Auto-unblock (>= Rs. 100)
 [PASS] M2-3: Admin blacklisting refuses tag at toll plaza
 [PASS] M3/4-1: Normal FASTag crossing opens barrier
 [PASS] M3/4-2: 24-hour return trip applies 50% discount automatically
 [PASS] M3/4-3: Optical AVC mismatch charges higher toll and flags for review
 [PASS] M3/4-4: Missing active tag diverted to Cash lane (Double Toll 2x)
 [PASS] M3/4-5: Insufficient balance diverted to Cash lane (Normal Toll)
 [PASS] M3/4-6: Anti-double-billing cooldown prevents charge within 2 minutes
 [PASS] M3/4-7: Emergency vehicle in Emergency lane pays Rs. 0
 [PASS] M4-1: Active Monthly Pass allows Rs. 0 toll crossing
 [PASS] M4-2: Nearest Rs. 5 rounding algorithm
 [PASS] M5-1: 7-day dispute window (<=7 days allowed, >7 days rejected)
 [PASS] M5-2: Approved dispute refunds money directly to FASTag balance
 [PASS] M5-3: Low balance alert triggers when balance < Rs. 200
 [PASS] M5-4: SQL Queries 1, 2, 3 in-memory aggregations
=========================================================================================
 INTEGRATION TEST SUMMARY: Total: 19 | Passed: 19 | Failed: 0
=========================================================================================
ALL INTEGRATION TESTS PASSED WITH 100% SUCCESS!
```
