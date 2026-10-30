package com.toll.module5.billing;

import java.time.LocalDate;
import java.util.Map;

/**
 * Interface contract for Member 5: Module 5 (Payments, Disputes and Reports).
 * Responsibilities:
 * - Recharge methods: UPI, Card, Net Banking (stores amount, date, method, status).
 * - Cash toll payments recorded with collecting lane operator.
 * - 7-day dispute window: Admin approved disputes refund to FASTag balance.
 * - Low balance alerts: Generated when balance falls below Rs. 200 after deduction.
 * - Daily plaza revenue & lane vehicle count reports.
 * - SQL Queries 1, 2, 3 in-memory aggregation.
 */
public interface IBillingReportingService {
    void recordRecharge(String rechargeId, String tagId, double amount, String method, String status);
    void recordCashPayment(String transactionId, double amount, String operatorName);
    void raiseDispute(String disputeId, String transactionId, String vehicleRegNo, double amount, String reason);
    boolean approveDispute(String disputeId, String adminRemarks);
    void triggerLowBalanceAlert(String tagId, double remainingBalance);

    // Reports
    double getDailyRevenue(int plazaId, LocalDate date);
    Map<Integer, Integer> getVehicleCountPerLane(int plazaId, LocalDate date);
}
