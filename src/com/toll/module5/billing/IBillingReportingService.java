package com.toll.module5.billing;

import com.toll.common.RechargeMethod;
import com.toll.common.VehicleType;
import com.toll.module5.billing.model.CashPaymentRecord;
import com.toll.module5.billing.model.Dispute;
import com.toll.module5.billing.model.LowBalanceAlert;
import com.toll.module5.billing.model.RechargeRecord;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Interface contract for Module 5 (Payments, Disputes and Reports).
 */
public interface IBillingReportingService {
    RechargeRecord processRecharge(String tagId, double amount, RechargeMethod method);
    void recordCashPayment(String transactionId, int plazaId, int laneNumber, double amount, String operatorName);

    // 7-day dispute lifecycle
    Dispute raiseDispute(String transactionId, String tagId, String vehicleRegNo, double amount,
                         LocalDateTime txnTime, String reason);
    boolean approveDispute(String disputeId, String adminRemarks);
    boolean rejectDispute(String disputeId, String adminRemarks);

    // Low balance alert (< Rs. 200)
    void triggerLowBalanceAlert(String tagId, String vehicleRegNo, double balanceAfterDeduction);

    // Audit logs
    List<Dispute> getAllDisputes();
    List<LowBalanceAlert> getAllAlerts();
    List<RechargeRecord> getAllRecharges();
    List<CashPaymentRecord> getAllCashPayments();

    // Standard Reports
    double getDailyRevenue(int plazaId, LocalDate date);
    Map<Integer, Integer> getVehicleCountPerLane(int plazaId, LocalDate date);

    // In-memory SQL Exercise Query Reports
    List<VehicleTypeRevenueReport> getQuery1VehicleTypeSummary();
    List<PlazaAuditReport> getQuery2PlazaPerformance();
    List<AccountPlazaUsageReport> getQuery3MultiPlazaAccounts();

    record VehicleTypeRevenueReport(VehicleType vehicleType, long totalTransactions, double totalRevenue, double averageToll) {}
    record PlazaAuditReport(int plazaId, String plazaName, long totalCrossings, double totalRevenue, long failedCrossings) {}
    record AccountPlazaUsageReport(String tagId, String vehicleRegNo, long totalCrossings, double totalDeducted, long distinctPlazas) {}
}
