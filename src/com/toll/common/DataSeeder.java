package com.toll.common;

import com.toll.module2.account.model.Vehicle;
import com.toll.module2.account.service.IAccountService;

/**
 * Utility to seed initial in-memory data for vehicles and FASTag accounts
 * per project instructions.
 */
public final class DataSeeder {

    private DataSeeder() {}

    public static void seedModule2Data(IAccountService accountService) {
        // 1. Car with healthy active balance
        accountService.registerVehicle(VehicleType.CAR, "MH-12-DE-1433", "Ramesh Sharma", "9876543210");
        accountService.issueFASTag("MH-12-DE-1433", "IND-TAG-1001", 1500.00);

        // 2. Car with low balance (< Rs. 100) -> automatically BLOCKED
        accountService.registerVehicle(VehicleType.CAR, "KA-01-MJ-5566", "Priya Nair", "9845123456");
        accountService.issueFASTag("KA-01-MJ-5566", "IND-TAG-1002", 85.00);

        // 3. Van with active balance
        accountService.registerVehicle(VehicleType.VAN, "DL-03-TC-9988", "Amit Verma", "9811223344");
        accountService.issueFASTag("DL-03-TC-9988", "IND-TAG-1003", 2500.00);

        // 4. Bus with commercial balance
        accountService.registerVehicle(VehicleType.BUS, "TN-09-BK-7711", "KPN Travels", "9444112233");
        accountService.issueFASTag("TN-09-BK-7711", "IND-TAG-1004", 8000.00);

        // 5. Heavy Truck with high balance
        accountService.registerVehicle(VehicleType.TRUCK, "GJ-06-ZZ-4321", "Patel Logistics", "9825098250");
        accountService.issueFASTag("GJ-06-ZZ-4321", "IND-TAG-1005", 12000.00);

        // 6. Multi-Axle Vehicle (4 Axles)
        accountService.registerMultiAxleVehicle("HR-26-BR-0007", "Jindal Freight Lines", "9911991100", 4);
        accountService.issueFASTag("HR-26-BR-0007", "IND-TAG-1006", 35000.00);

        // 7. Stolen / Blacklisted Tag
        accountService.registerVehicle(VehicleType.CAR, "TS-08-EF-9999", "Vikram Reddy", "9866001122");
        accountService.issueFASTag("TS-08-EF-9999", "IND-TAG-1007", 500.00);
        accountService.blacklistTag("IND-TAG-1007", "Reported stolen in Hyderabad cybercrime FIR #2026-44");

        // 8. Vehicle registered without a FASTag (for testing new issuance / Cash double toll)
        accountService.registerVehicle(VehicleType.CAR, "AP-03-XY-1234", "Suresh Kumar", "9700112233");
    }
}
