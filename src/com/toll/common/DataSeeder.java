package com.toll.common;

import com.toll.module1.plaza.IPlazaService;
import com.toll.module1.plaza.model.TollPlaza;
import com.toll.module2.account.service.IAccountService;
import com.toll.module3.tollengine.ITollProcessingService;
import com.toll.module3.tollengine.model.TollTransaction;
import com.toll.module3.tollengine.repository.ITransactionRepository;
import com.toll.module4.pricing.IPricingService;
import com.toll.module5.billing.IBillingReportingService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Master Data Seeder populating realistic Indian Highway initial data
 * across all 5 modules per project requirements.
 */
public final class DataSeeder {

    private DataSeeder() {}

    public static void seedModule2Data(IAccountService accountService) {
        accountService.registerVehicle(VehicleType.CAR, "MH-12-DE-1433", "Ramesh Sharma", "9876543210");
        accountService.issueFASTag("MH-12-DE-1433", "IND-TAG-1001", 1500.00);

        accountService.registerVehicle(VehicleType.CAR, "KA-01-MJ-5566", "Priya Nair", "9845123456");
        accountService.issueFASTag("KA-01-MJ-5566", "IND-TAG-1002", 85.00);

        accountService.registerVehicle(VehicleType.VAN, "DL-03-TC-9988", "Amit Verma", "9811223344");
        accountService.issueFASTag("DL-03-TC-9988", "IND-TAG-1003", 2500.00);

        accountService.registerVehicle(VehicleType.BUS, "TN-09-BK-7711", "KPN Travels", "9444112233");
        accountService.issueFASTag("TN-09-BK-7711", "IND-TAG-1004", 8000.00);

        accountService.registerVehicle(VehicleType.TRUCK, "GJ-06-ZZ-4321", "Patel Logistics", "9825098250");
        accountService.issueFASTag("GJ-06-ZZ-4321", "IND-TAG-1005", 12000.00);

        accountService.registerMultiAxleVehicle("HR-26-BR-0007", "Jindal Freight Lines", "9911991100", 4);
        accountService.issueFASTag("HR-26-BR-0007", "IND-TAG-1006", 35000.00);

        accountService.registerVehicle(VehicleType.CAR, "TS-08-EF-9999", "Vikram Reddy", "9866001122");
        accountService.issueFASTag("TS-08-EF-9999", "IND-TAG-1007", 500.00);
        accountService.blacklistTag("IND-TAG-1007", "Reported stolen in Hyderabad cybercrime FIR #2026-44");

        accountService.registerVehicle(VehicleType.CAR, "AP-03-XY-1234", "Suresh Kumar", "9700112233");
    }

    public static void seedAllModules(IPlazaService plazaService,
                                     IAccountService accountService,
                                     IPricingService pricingService,
                                     IBillingReportingService billingService,
                                     ITransactionRepository transactionRepository) {
        // -------------------------------------------------------------
        // 1. MODULE 1: TOLL PLAZAS, LANES & RATES
        // -------------------------------------------------------------
        LocalDate effectiveDate = LocalDate.now().minusDays(30);

        // Plaza 1: Khedshivapur Toll Plaza (NH-48, Pune-Satara)
        TollPlaza p1 = plazaService.createPlaza(1, "Khedshivapur Plaza", "NH-48", "Pune, Maharashtra");
        plazaService.addLane(1, 1, "FASTAG");
        plazaService.addLane(1, 2, "FASTAG");
        plazaService.addLane(1, 3, "CASH");
        plazaService.addLane(1, 4, "EMERGENCY");

        plazaService.setTollRate(1, VehicleType.CAR, TripType.SINGLE, 105.00, effectiveDate);
        plazaService.setTollRate(1, VehicleType.VAN, TripType.SINGLE, 165.00, effectiveDate);
        plazaService.setTollRate(1, VehicleType.BUS, TripType.SINGLE, 350.00, effectiveDate);
        plazaService.setTollRate(1, VehicleType.TRUCK, TripType.SINGLE, 370.00, effectiveDate);
        plazaService.setTollRate(1, VehicleType.MULTI_AXLE, TripType.SINGLE, 560.00, effectiveDate);
        plazaService.openPlaza(1, LocalDate.now());

        // Plaza 2: Kherki Daula Plaza (NH-48, Delhi-Gurgaon)
        TollPlaza p2 = plazaService.createPlaza(2, "Kherki Daula Plaza", "NH-48", "Gurgaon, Haryana");
        plazaService.addLane(2, 1, "FASTAG");
        plazaService.addLane(2, 2, "CASH");
        plazaService.addLane(2, 3, "EMERGENCY");

        plazaService.setTollRate(2, VehicleType.CAR, TripType.SINGLE, 80.00, effectiveDate);
        plazaService.setTollRate(2, VehicleType.VAN, TripType.SINGLE, 125.00, effectiveDate);
        plazaService.setTollRate(2, VehicleType.BUS, TripType.SINGLE, 245.00, effectiveDate);
        plazaService.setTollRate(2, VehicleType.TRUCK, TripType.SINGLE, 260.00, effectiveDate);
        plazaService.setTollRate(2, VehicleType.MULTI_AXLE, TripType.SINGLE, 410.00, effectiveDate);
        plazaService.openPlaza(2, LocalDate.now());

        // Plaza 3: Krishnagiri Plaza (NH-44, Bangalore-Chennai)
        TollPlaza p3 = plazaService.createPlaza(3, "Krishnagiri Plaza", "NH-44", "Krishnagiri, Tamil Nadu");
        plazaService.addLane(3, 1, "FASTAG");
        plazaService.addLane(3, 2, "FASTAG");
        plazaService.addLane(3, 3, "CASH");

        plazaService.setTollRate(3, VehicleType.CAR, TripType.SINGLE, 95.00, effectiveDate);
        plazaService.setTollRate(3, VehicleType.VAN, TripType.SINGLE, 150.00, effectiveDate);
        plazaService.setTollRate(3, VehicleType.BUS, TripType.SINGLE, 315.00, effectiveDate);
        plazaService.setTollRate(3, VehicleType.TRUCK, TripType.SINGLE, 335.00, effectiveDate);
        plazaService.setTollRate(3, VehicleType.MULTI_AXLE, TripType.SINGLE, 510.00, effectiveDate);
        plazaService.openPlaza(3, LocalDate.now());

        // -------------------------------------------------------------
        // 2. MODULE 2: VEHICLES & FASTAG ACCOUNTS
        // -------------------------------------------------------------
        // Car 1 (Active balance)
        accountService.registerVehicle(VehicleType.CAR, "MH-12-DE-1433", "Ramesh Sharma", "9876543210");
        accountService.issueFASTag("MH-12-DE-1433", "IND-TAG-1001", 2500.00);

        // Car 2 (Blocked balance < Rs. 100)
        accountService.registerVehicle(VehicleType.CAR, "KA-01-MJ-5566", "Priya Nair", "9845123456");
        accountService.issueFASTag("KA-01-MJ-5566", "IND-TAG-1002", 85.00);

        // Van (Active balance)
        accountService.registerVehicle(VehicleType.VAN, "DL-03-TC-9988", "Amit Verma", "9811223344");
        accountService.issueFASTag("DL-03-TC-9988", "IND-TAG-1003", 1800.00);

        // Bus (High balance)
        accountService.registerVehicle(VehicleType.BUS, "TN-09-BK-7711", "KPN Travels", "9444112233");
        accountService.issueFASTag("TN-09-BK-7711", "IND-TAG-1004", 12000.00);

        // Truck (High balance)
        accountService.registerVehicle(VehicleType.TRUCK, "GJ-06-ZZ-4321", "Patel Logistics", "9825098250");
        accountService.issueFASTag("GJ-06-ZZ-4321", "IND-TAG-1005", 15000.00);

        // Multi-Axle Vehicle
        accountService.registerMultiAxleVehicle("HR-26-BR-0007", "Jindal Freight Lines", "9911991100", 4);
        accountService.issueFASTag("HR-26-BR-0007", "IND-TAG-1006", 38000.00);

        // Stolen / Blacklisted Tag
        accountService.registerVehicle(VehicleType.CAR, "TS-08-EF-9999", "Vikram Reddy", "9866001122");
        accountService.issueFASTag("TS-08-EF-9999", "IND-TAG-1007", 600.00);
        accountService.blacklistTag("IND-TAG-1007", "Stolen tag FIR #HYD-2026-99");

        // Vehicle without FASTag
        accountService.registerVehicle(VehicleType.CAR, "AP-03-XY-1234", "Suresh Kumar", "9700112233");

        // -------------------------------------------------------------
        // 3. MODULE 4: MONTHLY PASSES
        // -------------------------------------------------------------
        // Monthly pass for Ramesh Sharma's car at Plaza 1
        pricingService.issueMonthlyPass("PASS-P1-1001", "MH-12-DE-1433", 1,
                LocalDate.now().minusDays(10), LocalDate.now().plusDays(20), 3000.00);

        // -------------------------------------------------------------
        // 4. HISTORICAL TRANSACTIONS FOR SQL QUERIES & RETURN TRIPS
        // -------------------------------------------------------------
        LocalDateTime yesterday = LocalDateTime.now().minusHours(5);
        LocalDateTime twoDaysAgo = LocalDateTime.now().minusDays(2);
        LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);

        // Single trip yesterday for Van DL-03-TC-9988 at Plaza 1 (eligible for 24h return discount today!)
        transactionRepository.save(new TollTransaction(UUID.randomUUID().toString(), "DL-03-TC-9988", 1, 1,
                "IND-TAG-1003", yesterday, 165.00, VehicleType.VAN, VehicleType.VAN,
                TripType.SINGLE, PaymentType.FASTAG, TransactionStatus.SUCCESS, false, null, null));

        // Multiple crossings across Plazas 1, 2, and 3 for Bus TN-09-BK-7711 (satisfies Query 3 >2 distinct plazas)
        transactionRepository.save(new TollTransaction(UUID.randomUUID().toString(), "TN-09-BK-7711", 1, 1,
                "IND-TAG-1004", threeDaysAgo, 350.00, VehicleType.BUS, VehicleType.BUS,
                TripType.SINGLE, PaymentType.FASTAG, TransactionStatus.SUCCESS, false, null, null));
        transactionRepository.save(new TollTransaction(UUID.randomUUID().toString(), "TN-09-BK-7711", 2, 1,
                "IND-TAG-1004", twoDaysAgo, 245.00, VehicleType.BUS, VehicleType.BUS,
                TripType.SINGLE, PaymentType.FASTAG, TransactionStatus.SUCCESS, false, null, null));
        transactionRepository.save(new TollTransaction(UUID.randomUUID().toString(), "TN-09-BK-7711", 3, 1,
                "IND-TAG-1004", yesterday, 315.00, VehicleType.BUS, VehicleType.BUS,
                TripType.SINGLE, PaymentType.FASTAG, TransactionStatus.SUCCESS, false, null, null));

        // Multi-Axle HR-26-BR-0007 transactions across Plazas 1, 2, and 3
        transactionRepository.save(new TollTransaction(UUID.randomUUID().toString(), "HR-26-BR-0007", 1, 2,
                "IND-TAG-1006", threeDaysAgo, 560.00, VehicleType.MULTI_AXLE, VehicleType.MULTI_AXLE,
                TripType.SINGLE, PaymentType.FASTAG, TransactionStatus.SUCCESS, false, null, null));
        transactionRepository.save(new TollTransaction(UUID.randomUUID().toString(), "HR-26-BR-0007", 2, 1,
                "IND-TAG-1006", twoDaysAgo, 410.00, VehicleType.MULTI_AXLE, VehicleType.MULTI_AXLE,
                TripType.SINGLE, PaymentType.FASTAG, TransactionStatus.SUCCESS, false, null, null));
        transactionRepository.save(new TollTransaction(UUID.randomUUID().toString(), "HR-26-BR-0007", 3, 1,
                "IND-TAG-1006", yesterday, 510.00, VehicleType.MULTI_AXLE, VehicleType.MULTI_AXLE,
                TripType.SINGLE, PaymentType.FASTAG, TransactionStatus.SUCCESS, false, null, null));

        // Truck GJ-06-ZZ-4321 crossings (Truck vehicle type >2 successful txns for Query 1)
        transactionRepository.save(new TollTransaction(UUID.randomUUID().toString(), "GJ-06-ZZ-4321", 1, 1,
                "IND-TAG-1005", threeDaysAgo, 370.00, VehicleType.TRUCK, VehicleType.TRUCK,
                TripType.SINGLE, PaymentType.FASTAG, TransactionStatus.SUCCESS, false, null, null));
        transactionRepository.save(new TollTransaction(UUID.randomUUID().toString(), "GJ-06-ZZ-4321", 1, 2,
                "IND-TAG-1005", twoDaysAgo, 370.00, VehicleType.TRUCK, VehicleType.TRUCK,
                TripType.SINGLE, PaymentType.FASTAG, TransactionStatus.SUCCESS, false, null, null));
        transactionRepository.save(new TollTransaction(UUID.randomUUID().toString(), "GJ-06-ZZ-4321", 2, 1,
                "IND-TAG-1005", yesterday, 260.00, VehicleType.TRUCK, VehicleType.TRUCK,
                TripType.SINGLE, PaymentType.FASTAG, TransactionStatus.SUCCESS, false, null, null));

        // A failed transaction at Plaza 1 for Query 2 testing
        transactionRepository.save(new TollTransaction(UUID.randomUUID().toString(), "KA-01-MJ-5566", 1, 1,
                "IND-TAG-1002", yesterday, 105.00, VehicleType.CAR, VehicleType.CAR,
                TripType.SINGLE, PaymentType.FASTAG, TransactionStatus.FAILED, false, null,
                "Insufficient balance in FASTag"));
    }
}
