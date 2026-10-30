package com.toll.module5.billing;

import com.toll.common.*;
import com.toll.module1.plaza.repository.IPlazaRepository;
import com.toll.module2.account.service.IAccountService;
import com.toll.module2.account.service.RechargeResult;
import com.toll.module3.tollengine.model.TollTransaction;
import com.toll.module3.tollengine.repository.ITransactionRepository;
import com.toll.module5.billing.model.CashPaymentRecord;
import com.toll.module5.billing.model.Dispute;
import com.toll.module5.billing.model.LowBalanceAlert;
import com.toll.module5.billing.model.RechargeRecord;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Implementation of IBillingReportingService for Module 5.
 * Handles recharges, cash operator records, 7-day disputes, low-balance alerts,
 * daily revenue and vehicle counts, plus in-memory execution of SQL Queries 1, 2, and 3.
 */
public class BillingReportingServiceImpl implements IBillingReportingService {
    private final IAccountService accountService;
    private final ITransactionRepository transactionRepository;
    private final IPlazaRepository plazaRepository;

    private final Map<String, Dispute> disputeStore = new ConcurrentHashMap<>();
    private final List<LowBalanceAlert> alertStore = new CopyOnWriteArrayList<>();
    private final List<RechargeRecord> rechargeStore = new CopyOnWriteArrayList<>();
    private final List<CashPaymentRecord> cashStore = new CopyOnWriteArrayList<>();

    public BillingReportingServiceImpl(IAccountService accountService,
                                       ITransactionRepository transactionRepository,
                                       IPlazaRepository plazaRepository) {
        this.accountService = accountService;
        this.transactionRepository = transactionRepository;
        this.plazaRepository = plazaRepository;
    }

    @Override
    public RechargeRecord processRecharge(String tagId, double amount, RechargeMethod method) {
        String rechargeId = "RCH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        RechargeResult result = accountService.rechargeAccount(tagId, amount);

        RechargeStatus status = result.isSuccessful() ? RechargeStatus.PAID : RechargeStatus.FAILED;
        String failureReason = result.isSuccessful() ? null : result.getMessage();

        RechargeRecord record = new RechargeRecord(rechargeId, tagId, amount, method, status, LocalDateTime.now(), failureReason);
        rechargeStore.add(record);
        return record;
    }

    @Override
    public void recordCashPayment(String transactionId, int plazaId, int laneNumber, double amount, String operatorName) {
        String receiptId = "RCPT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        CashPaymentRecord record = new CashPaymentRecord(receiptId, transactionId, plazaId, laneNumber, amount,
                operatorName, LocalDateTime.now());
        cashStore.add(record);
    }

    @Override
    public Dispute raiseDispute(String transactionId, String tagId, String vehicleRegNo, double amount,
                                LocalDateTime txnTime, String reason) {
        // 7-day dispute window rule
        long daysDiff = ChronoUnit.DAYS.between(txnTime, LocalDateTime.now());
        if (daysDiff > 7) {
            throw new IllegalArgumentException(String.format(
                    "Dispute rejected: Transaction is %d days old. Disputes must be filed within 7 days.", daysDiff));
        }

        String disputeId = "DISP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Dispute dispute = new Dispute(disputeId, transactionId, tagId, vehicleRegNo, amount, reason);
        disputeStore.put(disputeId, dispute);
        return dispute;
    }

    @Override
    public synchronized boolean approveDispute(String disputeId, String adminRemarks) {
        Dispute dispute = disputeStore.get(disputeId);
        if (dispute == null) throw new IllegalArgumentException("Dispute #" + disputeId + " not found.");
        if (dispute.getStatus() != DisputeStatus.RAISED) {
            throw new IllegalStateException("Dispute has already been " + dispute.getStatus());
        }

        dispute.approve(adminRemarks);
        // Automatically refund disputed amount to the FASTag balance
        if (dispute.getTagId() != null) {
            accountService.refundAccount(dispute.getTagId(), dispute.getDisputedAmount(), adminRemarks);
        }
        return true;
    }

    @Override
    public synchronized boolean rejectDispute(String disputeId, String adminRemarks) {
        Dispute dispute = disputeStore.get(disputeId);
        if (dispute == null) throw new IllegalArgumentException("Dispute #" + disputeId + " not found.");
        if (dispute.getStatus() != DisputeStatus.RAISED) {
            throw new IllegalStateException("Dispute has already been " + dispute.getStatus());
        }
        dispute.reject(adminRemarks);
        return true;
    }

    @Override
    public void triggerLowBalanceAlert(String tagId, String vehicleRegNo, double balanceAfterDeduction) {
        String alertId = "ALT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String message = String.format("LOW BALANCE WARNING: FASTag %s balance is Rs. %.2f (< Rs. 200.00 threshold).",
                tagId, balanceAfterDeduction);
        LowBalanceAlert alert = new LowBalanceAlert(alertId, tagId, vehicleRegNo, balanceAfterDeduction, LocalDateTime.now(), message);
        alertStore.add(alert);
    }

    @Override
    public List<Dispute> getAllDisputes() {
        return new ArrayList<>(disputeStore.values());
    }

    @Override
    public List<LowBalanceAlert> getAllAlerts() {
        return Collections.unmodifiableList(alertStore);
    }

    @Override
    public List<RechargeRecord> getAllRecharges() {
        return Collections.unmodifiableList(rechargeStore);
    }

    @Override
    public List<CashPaymentRecord> getAllCashPayments() {
        return Collections.unmodifiableList(cashStore);
    }

    @Override
    public double getDailyRevenue(int plazaId, LocalDate date) {
        return transactionRepository.findByPlaza(plazaId).stream()
                .filter(t -> t.isSuccessful() && t.timestamp().toLocalDate().isEqual(date))
                .mapToDouble(TollTransaction::tollAmount)
                .sum();
    }

    @Override
    public Map<Integer, Integer> getVehicleCountPerLane(int plazaId, LocalDate date) {
        return transactionRepository.findByPlaza(plazaId).stream()
                .filter(t -> t.timestamp().toLocalDate().isEqual(date))
                .collect(Collectors.groupingBy(TollTransaction::laneNumber, Collectors.summingInt(t -> 1)));
    }

    // --- In-Memory SQL Query Implementations ---

    /**
     * Query 1:
     * For each vehicle type, find the total number of toll transactions, the total toll collected,
     * and the average toll per crossing.
     * Show only vehicle types that have more than 2 successful transactions.
     * Sort by total toll collected in descending order.
     */
    @Override
    public List<VehicleTypeRevenueReport> getQuery1VehicleTypeSummary() {
        Map<VehicleType, List<TollTransaction>> grouped = transactionRepository.findAll().stream()
                .filter(TollTransaction::isSuccessful)
                .collect(Collectors.groupingBy(TollTransaction::billedType));

        List<VehicleTypeRevenueReport> reports = new ArrayList<>();
        for (Map.Entry<VehicleType, List<TollTransaction>> entry : grouped.entrySet()) {
            List<TollTransaction> txns = entry.getValue();
            if (txns.size() > 2) { // Filter: more than 2 successful transactions
                double total = txns.stream().mapToDouble(TollTransaction::tollAmount).sum();
                double avg = total / txns.size();
                reports.add(new VehicleTypeRevenueReport(entry.getKey(), txns.size(), total, avg));
            }
        }
        reports.sort((a, b) -> Double.compare(b.totalRevenue(), a.totalRevenue())); // DESC
        return reports;
    }

    /**
     * Query 2:
     * For each toll plaza, list the total number of crossings, the total revenue collected,
     * and the number of failed transactions.
     * Group the results by plaza and include only plazas that have at least 3 crossings.
     */
    @Override
    public List<PlazaAuditReport> getQuery2PlazaPerformance() {
        Map<Integer, List<TollTransaction>> grouped = transactionRepository.findAll().stream()
                .collect(Collectors.groupingBy(TollTransaction::plazaId));

        List<PlazaAuditReport> reports = new ArrayList<>();
        for (Map.Entry<Integer, List<TollTransaction>> entry : grouped.entrySet()) {
            int plazaId = entry.getKey();
            List<TollTransaction> txns = entry.getValue();
            if (txns.size() >= 3) { // Filter: at least 3 crossings
                double revenue = txns.stream().filter(TollTransaction::isSuccessful).mapToDouble(TollTransaction::tollAmount).sum();
                long failed = txns.stream().filter(t -> t.status() == TransactionStatus.FAILED).count();
                String name = plazaRepository.findById(plazaId).map(p -> p.getName() + " (" + p.getHighwayName() + ")").orElse("Plaza #" + plazaId);
                reports.add(new PlazaAuditReport(plazaId, name, txns.size(), revenue, failed));
            }
        }
        return reports;
    }

    /**
     * Query 3:
     * For each FASTag account, find the total number of crossings, the total amount deducted,
     * and the number of distinct plazas used.
     * Include only accounts that crossed at more than 2 distinct plazas,
     * ordered by total amount deducted in descending order.
     */
    @Override
    public List<AccountPlazaUsageReport> getQuery3MultiPlazaAccounts() {
        Map<String, List<TollTransaction>> grouped = transactionRepository.findAll().stream()
                .filter(t -> t.tagId() != null && t.isSuccessful())
                .collect(Collectors.groupingBy(TollTransaction::tagId));

        List<AccountPlazaUsageReport> reports = new ArrayList<>();
        for (Map.Entry<String, List<TollTransaction>> entry : grouped.entrySet()) {
            String tagId = entry.getKey();
            List<TollTransaction> txns = entry.getValue();
            long distinctPlazas = txns.stream().map(TollTransaction::plazaId).distinct().count();

            if (distinctPlazas > 2) { // Filter: more than 2 distinct plazas
                double total = txns.stream().mapToDouble(TollTransaction::tollAmount).sum();
                String regNo = txns.get(0).vehicleRegNo();
                reports.add(new AccountPlazaUsageReport(tagId, regNo, txns.size(), total, distinctPlazas));
            }
        }
        reports.sort((a, b) -> Double.compare(b.totalDeducted(), a.totalDeducted())); // DESC
        return reports;
    }
}
