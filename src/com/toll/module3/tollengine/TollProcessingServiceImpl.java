package com.toll.module3.tollengine;

import com.toll.common.AccountStatus;
import com.toll.common.DataSeeder;
import com.toll.common.VehicleType;
import com.toll.module2.account.model.FASTagAccount;
import com.toll.module2.account.model.Vehicle;
import com.toll.module2.account.repository.InMemoryFASTagAccountRepository;
import com.toll.module2.account.repository.InMemoryVehicleRepository;
import com.toll.module2.account.service.AccountServiceImpl;
import com.toll.module2.account.service.DebitResult;
import com.toll.module2.account.service.IAccountService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Enterprise Toll Processing Engine Implementation (Module 3).
 * Integrates with Module 2 IAccountService for vehicle and FASTag identity,
 * and executes statutory NHAI rules (anti-tailgating 2-min cooldown, AVC mismatch,
 * cash fallback routing with 2x penalty or normal toll, and emergency bypass).
 */
public class TollProcessingServiceImpl implements ITollProcessingService {

    private final IAccountService accountService;

    // In-memory runtime state stores
    private final Map<String, LocalDateTime> lastChargedMap = new ConcurrentHashMap<>();
    private final Map<String, Double> ratesMap = new ConcurrentHashMap<>();
    private final Map<String, Boolean> passMap = new ConcurrentHashMap<>();
    private final Map<String, BarrierGate> barriersMap = new ConcurrentHashMap<>();
    private final Map<String, Boolean> laneStatusMap = new ConcurrentHashMap<>();
    private final List<TollTransaction> transactionHistory = new CopyOnWriteArrayList<>();

    public TollProcessingServiceImpl(IAccountService accountService) {
        this.accountService = Objects.requireNonNull(accountService, "AccountService cannot be null.");
        initDefaultRates();
    }

    public TollProcessingServiceImpl() {
        this.accountService = new AccountServiceImpl(
                new InMemoryVehicleRepository(),
                new InMemoryFASTagAccountRepository()
        );
        DataSeeder.seedModule2Data(this.accountService);
        initDefaultRates();
    }

    private void initDefaultRates() {
        // Standard NHAI base single trip rates for Plaza 1 (NH-48) & Plaza 2 (NH-44)
        registerBaseRate(1, VehicleType.CAR, 80.0);
        registerBaseRate(1, VehicleType.VAN, 130.0);
        registerBaseRate(1, VehicleType.BUS, 270.0);
        registerBaseRate(1, VehicleType.TRUCK, 270.0);
        registerBaseRate(1, VehicleType.MULTI_AXLE, 420.0);

        registerBaseRate(2, VehicleType.CAR, 90.0);
        registerBaseRate(2, VehicleType.VAN, 145.0);
        registerBaseRate(2, VehicleType.BUS, 290.0);
        registerBaseRate(2, VehicleType.TRUCK, 290.0);
        registerBaseRate(2, VehicleType.MULTI_AXLE, 450.0);
    }

    private double roundToNearest5(double amount) {
        return Math.round(amount / 5.0) * 5.0;
    }

    private String laneKey(int plazaId, int laneNumber) {
        return plazaId + ":" + laneNumber;
    }

    private String rateKey(int plazaId, VehicleType type) {
        return plazaId + ":" + type.name();
    }

    private String passKey(String vehicleRegNo, int plazaId) {
        return vehicleRegNo.trim().toUpperCase() + "@" + plazaId;
    }

    private String cooldownKey(String tagId, int plazaId) {
        return tagId.trim().toUpperCase() + "@" + plazaId;
    }

    @Override
    public BarrierGate getBarrier(int plazaId, int laneNumber) {
        return barriersMap.computeIfAbsent(laneKey(plazaId, laneNumber),
                k -> new BarrierGate(plazaId, laneNumber));
    }

    private boolean isLaneOpen(int plazaId, int laneNumber) {
        Boolean open = laneStatusMap.get(laneKey(plazaId, laneNumber));
        return open == null || open;
    }

    @Override
    public CrossingResult processCrossing(int plazaId, int laneNumber, String tagId, VehicleType observedType) {
        return processCrossing(plazaId, laneNumber, tagId, observedType, LocalDateTime.now());
    }

    @Override
    public CrossingResult processCrossing(int plazaId, int laneNumber, String tagId, 
                                          VehicleType observedType, LocalDateTime crossingTime) {
        BarrierGate barrier = getBarrier(plazaId, laneNumber);
        String txnId = UUID.randomUUID().toString();

        // 1. Check Lane Open Status
        if (!isLaneOpen(plazaId, laneNumber)) {
            barrier.closeBarrier();
            TollTransaction tx = TollTransaction.builder()
                    .transactionId(txnId)
                    .plazaId(plazaId)
                    .laneNumber(laneNumber)
                    .vehicleRegNo("UNKNOWN")
                    .tagId(tagId)
                    .timestamp(crossingTime)
                    .status("FAILED")
                    .failureReason("Lane #" + laneNumber + " is currently CLOSED")
                    .build();
            transactionHistory.add(tx);
            return new CrossingResult(false, txnId, 0.0, "FAILED", 
                    "Lane #" + laneNumber + " is currently CLOSED.", false);
        }

        // 2. Resolve Tag & Account
        Optional<FASTagAccount> accOpt = (tagId != null && !tagId.trim().isEmpty()) ?
                accountService.findAccountByTagId(tagId) : Optional.empty();

        VehicleType obsType = (observedType != null) ? observedType : VehicleType.CAR;

        // Rule: Vehicle without a valid Active tag is sent to a Cash lane and pays double the toll (2x)
        if (accOpt.isEmpty() || accOpt.get().getStatus() == AccountStatus.CLOSED) {
            barrier.closeBarrier();
            double baseRate = getBaseRate(plazaId, obsType);
            double penaltyToll = roundToNearest5(2.0 * baseRate);

            String vehicleReg = accOpt.map(FASTagAccount::getVehicleRegistrationNumber).orElse("UNREGISTERED");

            TollTransaction tx = TollTransaction.builder()
                    .transactionId(txnId)
                    .plazaId(plazaId)
                    .laneNumber(laneNumber)
                    .vehicleRegNo(vehicleReg)
                    .tagId(tagId)
                    .timestamp(crossingTime)
                    .tollAmount(penaltyToll)
                    .observedType(obsType)
                    .billedType(obsType)
                    .tripType(TripType.SINGLE)
                    .paymentMethod(PaymentMethod.FASTAG)
                    .status("FAILED")
                    .failureReason("No valid active tag. Diverted to Cash Lane (Double Toll penalty applied)")
                    .build();
            transactionHistory.add(tx);

            return new CrossingResult(false, txnId, penaltyToll, "FAILED",
                    "No valid active FASTag detected. Diverted to Cash Lane (Double Toll: Rs. " + penaltyToll + ")", false);
        }

        FASTagAccount account = accOpt.get();
        String vehicleRegNo = account.getVehicleRegistrationNumber();

        // 3. Blacklist Check
        if (account.isBlacklisted()) {
            barrier.closeBarrier();
            TollTransaction tx = TollTransaction.builder()
                    .transactionId(txnId)
                    .plazaId(plazaId)
                    .laneNumber(laneNumber)
                    .vehicleRegNo(vehicleRegNo)
                    .tagId(account.getTagId())
                    .timestamp(crossingTime)
                    .tollAmount(0.0)
                    .observedType(obsType)
                    .status("FAILED")
                    .failureReason("FASTag [" + account.getTagId() + "] is BLACKLISTED (" + account.getBlacklistReason() + ")")
                    .build();
            transactionHistory.add(tx);

            return new CrossingResult(false, txnId, 0.0, "FAILED",
                    "ALARM: FASTag [" + account.getTagId() + "] is BLACKLISTED! Refused at Plaza.", false);
        }

        // 4. Anti-Double-Billing / 2-Minute Cooldown Check
        if (isCooldownActive(account.getTagId(), plazaId, crossingTime)) {
            barrier.closeBarrier();
            long remSecs = getRemainingCooldownSeconds(account.getTagId(), plazaId, crossingTime);
            TollTransaction tx = TollTransaction.builder()
                    .transactionId(txnId)
                    .plazaId(plazaId)
                    .laneNumber(laneNumber)
                    .vehicleRegNo(vehicleRegNo)
                    .tagId(account.getTagId())
                    .timestamp(crossingTime)
                    .tollAmount(0.0)
                    .observedType(obsType)
                    .status("FAILED")
                    .failureReason("Anti-tailgating / 2-minute cooldown active (" + remSecs + "s remaining)")
                    .build();
            transactionHistory.add(tx);

            return new CrossingResult(false, txnId, 0.0, "FAILED",
                    "Duplicate crossing rejected: Tag charged at this plaza within 2 minutes (" + remSecs + "s remaining).", false);
        }

        // 5. Monthly Pass Exemption Check
        if (Boolean.TRUE.equals(passMap.get(passKey(vehicleRegNo, plazaId)))) {
            barrier.openBarrier();
            lastChargedMap.put(cooldownKey(account.getTagId(), plazaId), crossingTime);

            TollTransaction tx = TollTransaction.builder()
                    .transactionId(txnId)
                    .plazaId(plazaId)
                    .laneNumber(laneNumber)
                    .vehicleRegNo(vehicleRegNo)
                    .tagId(account.getTagId())
                    .timestamp(crossingTime)
                    .tollAmount(0.0)
                    .observedType(obsType)
                    .billedType(obsType)
                    .tripType(TripType.PASS)
                    .paymentMethod(PaymentMethod.EXEMPT)
                    .status("SUCCESS")
                    .build();
            transactionHistory.add(tx);

            return new CrossingResult(true, txnId, 0.0, "SUCCESS",
                    "Valid Monthly Pass verified: Toll = Rs. 0.00. Boom barrier OPEN.", false);
        }

        // 6. Registered Vehicle Type & AVC Mismatch Evaluation
        Optional<Vehicle> vehOpt = accountService.findVehicleByRegistration(vehicleRegNo);
        VehicleType regType = vehOpt.map(Vehicle::getVehicleType).orElse(obsType);

        double regBaseRate = getBaseRate(plazaId, regType);
        double obsBaseRate = getBaseRate(plazaId, obsType);

        boolean isMismatch = (regType != obsType);
        double applicableSingleRate = Math.max(regBaseRate, obsBaseRate);
        VehicleType billedType = (obsBaseRate >= regBaseRate) ? obsType : regType;

        // Check 24-hour return trip discount (50% of single rate)
        boolean isReturn = isReturnTripWithin24Hours(vehicleRegNo, plazaId, crossingTime);
        TripType tripType = isReturn ? TripType.RETURN : TripType.SINGLE;
        double unroundedToll = isReturn ? (applicableSingleRate * 0.50) : applicableSingleRate;
        double finalToll = roundToNearest5(unroundedToll);

        // 7. Balance Verification & Cash Fallback Routing
        if (account.getStatus() != AccountStatus.ACTIVE || account.getBalance() < finalToll) {
            barrier.closeBarrier();

            TollTransaction tx = TollTransaction.builder()
                    .transactionId(txnId)
                    .plazaId(plazaId)
                    .laneNumber(laneNumber)
                    .vehicleRegNo(vehicleRegNo)
                    .tagId(account.getTagId())
                    .timestamp(crossingTime)
                    .tollAmount(finalToll)
                    .observedType(obsType)
                    .billedType(billedType)
                    .tripType(tripType)
                    .paymentMethod(PaymentMethod.FASTAG)
                    .status("FAILED")
                    .failureReason(String.format("Insufficient balance (Rs. %.2f < Rs. %.2f) / Account Blocked", 
                            account.getBalance(), finalToll))
                    .flaggedForReview(isMismatch)
                    .build();
            transactionHistory.add(tx);

            return new CrossingResult(false, txnId, finalToll, "FAILED",
                    String.format("Insufficient FASTag balance (Rs. %.2f < Rs. %.2f). Diverted to Cash Lane (Normal Toll: Rs. %.2f)",
                            account.getBalance(), finalToll, finalToll), isMismatch);
        }

        // 8. Execute Debit via Module 2 AccountService
        DebitResult debitResult = accountService.deductToll(account.getTagId(), finalToll);
        if (!debitResult.isSuccessful()) {
            barrier.closeBarrier();
            return new CrossingResult(false, txnId, finalToll, "FAILED",
                    "Debit failed: " + debitResult.getMessage() + ". Diverted to Cash Lane (Normal Toll: Rs. " + finalToll + ")", isMismatch);
        }

        // Update cooldown timestamp & Open Barrier
        lastChargedMap.put(cooldownKey(account.getTagId(), plazaId), crossingTime);
        barrier.openBarrier();

        TollTransaction tx = TollTransaction.builder()
                .transactionId(txnId)
                .plazaId(plazaId)
                .laneNumber(laneNumber)
                .vehicleRegNo(vehicleRegNo)
                .tagId(account.getTagId())
                .timestamp(crossingTime)
                .tollAmount(finalToll)
                .observedType(obsType)
                .billedType(billedType)
                .tripType(tripType)
                .paymentMethod(PaymentMethod.FASTAG)
                .status("SUCCESS")
                .flaggedForReview(isMismatch)
                .build();
        transactionHistory.add(tx);

        String successMsg = String.format("FASTag Toll Deducted: Rs. %.2f | Trip: %s | Remaining Bal: Rs. %.2f | Barrier OPEN%s%s%s",
                finalToll, tripType, debitResult.getRemainingBalance(),
                debitResult.isAutoBlocked() ? " [ALERT: Auto-Blocked (< Rs. 100)]" : "",
                debitResult.isLowBalanceAlertTriggered() ? " [ALERT: Low Balance (< Rs. 200)]" : "",
                isMismatch ? " [FLAG: AVC Mismatch Detected - Higher Toll Charged]" : "");

        return new CrossingResult(true, txnId, finalToll, "SUCCESS", successMsg, isMismatch);
    }

    @Override
    public CrossingResult processEmergencyCrossing(int plazaId, int laneNumber, String vehicleRegNo) {
        return processEmergencyCrossing(plazaId, laneNumber, vehicleRegNo, LocalDateTime.now());
    }

    @Override
    public CrossingResult processEmergencyCrossing(int plazaId, int laneNumber, 
                                                   String vehicleRegNo, LocalDateTime crossingTime) {
        BarrierGate barrier = getBarrier(plazaId, laneNumber);
        String txnId = UUID.randomUUID().toString();
        String reg = (vehicleRegNo != null && !vehicleRegNo.trim().isEmpty()) ? vehicleRegNo.trim().toUpperCase() : "UNKNOWN";

        // Authorized emergency check: Ambulance, Fire Engine, or reg prefix
        boolean isAuthorizedEmergency = reg.contains("AMB") || reg.contains("FIRE") || reg.contains("EMG") ||
                reg.contains("108") || reg.contains("101");

        if (!isAuthorizedEmergency) {
            barrier.closeBarrier();
            TollTransaction tx = TollTransaction.builder()
                    .transactionId(txnId)
                    .plazaId(plazaId)
                    .laneNumber(laneNumber)
                    .vehicleRegNo(reg)
                    .timestamp(crossingTime)
                    .tollAmount(0.0)
                    .tripType(TripType.EMERGENCY)
                    .paymentMethod(PaymentMethod.EXEMPT)
                    .status("FAILED")
                    .failureReason("Non-emergency vehicle unauthorized in Emergency Lane")
                    .build();
            transactionHistory.add(tx);

            return new CrossingResult(false, txnId, 0.0, "FAILED",
                    "Crossing Refused: Non-emergency vehicle unauthorized in Emergency Lane!", false);
        }

        barrier.openBarrier();
        TollTransaction tx = TollTransaction.builder()
                .transactionId(txnId)
                .plazaId(plazaId)
                .laneNumber(laneNumber)
                .vehicleRegNo(reg)
                .timestamp(crossingTime)
                .tollAmount(0.0)
                .tripType(TripType.EMERGENCY)
                .paymentMethod(PaymentMethod.EXEMPT)
                .status("SUCCESS")
                .build();
        transactionHistory.add(tx);

        return new CrossingResult(true, txnId, 0.0, "SUCCESS",
                "Emergency Vehicle Authorized: Toll = Rs. 0.00. Boom barrier OPEN.", false);
    }

    @Override
    public CrossingResult processCashCrossing(int plazaId, int laneNumber, String vehicleRegNo, 
                                             VehicleType vehicleType, String operatorName, boolean isDoubleTollPenalty) {
        return processCashCrossing(plazaId, laneNumber, vehicleRegNo, vehicleType, operatorName, isDoubleTollPenalty, LocalDateTime.now());
    }

    @Override
    public CrossingResult processCashCrossing(int plazaId, int laneNumber, String vehicleRegNo, 
                                             VehicleType vehicleType, String operatorName, 
                                             boolean isDoubleTollPenalty, LocalDateTime crossingTime) {
        BarrierGate barrier = getBarrier(plazaId, laneNumber);
        String txnId = UUID.randomUUID().toString();
        VehicleType type = (vehicleType != null) ? vehicleType : VehicleType.CAR;

        double baseRate = getBaseRate(plazaId, type);
        double rawAmount = isDoubleTollPenalty ? (2.0 * baseRate) : baseRate;
        double finalCashToll = roundToNearest5(rawAmount);

        String reg = (vehicleRegNo != null && !vehicleRegNo.trim().isEmpty()) ? vehicleRegNo.trim().toUpperCase() : "CASH-VEHICLE";
        String op = (operatorName != null && !operatorName.trim().isEmpty()) ? operatorName.trim() : "Operator-General";

        barrier.openBarrier();

        TollTransaction tx = TollTransaction.builder()
                .transactionId(txnId)
                .plazaId(plazaId)
                .laneNumber(laneNumber)
                .vehicleRegNo(reg)
                .timestamp(crossingTime)
                .tollAmount(finalCashToll)
                .observedType(type)
                .billedType(type)
                .tripType(TripType.SINGLE)
                .paymentMethod(PaymentMethod.CASH)
                .status("SUCCESS")
                .operatorName(op)
                .build();
        transactionHistory.add(tx);

        return new CrossingResult(true, txnId, finalCashToll, "SUCCESS",
                String.format("Cash Payment Settled: Rs. %.2f (%s) collected by Operator [%s]. Barrier OPEN.",
                        finalCashToll, isDoubleTollPenalty ? "Double Toll 2x" : "Normal Toll", op), false);
    }

    @Override
    public CrossingResult processDivertedVehicleInCash(CrossingResult divertedResult, int cashLaneNumber, String operatorName) {
        if (divertedResult == null || !divertedResult.isDivertedToCash()) {
            throw new IllegalArgumentException("Provided crossing result was not diverted to Cash lane.");
        }

        // Find transaction from history
        Optional<TollTransaction> txOpt = transactionHistory.stream()
                .filter(t -> t.getTransactionId().equals(divertedResult.transactionId()))
                .findFirst();

        String reg = txOpt.map(TollTransaction::getVehicleRegNo).orElse("DIVERTED-VEHICLE");
        VehicleType obs = txOpt.map(TollTransaction::getObservedType).orElse(VehicleType.CAR);
        int plazaId = txOpt.map(TollTransaction::getPlazaId).orElse(1);

        return processCashCrossing(plazaId, cashLaneNumber, reg, obs, operatorName, divertedResult.isDoubleTollPenalty());
    }

    private boolean isReturnTripWithin24Hours(String vehicleRegNo, int plazaId, LocalDateTime crossingTime) {
        if (vehicleRegNo == null || vehicleRegNo.trim().isEmpty()) return false;
        String reg = vehicleRegNo.trim().toUpperCase();

        Optional<TollTransaction> lastTrip = transactionHistory.stream()
                .filter(tx -> tx.isSuccess() &&
                              tx.getPlazaId() == plazaId &&
                              tx.getVehicleRegNo().equalsIgnoreCase(reg) &&
                              !tx.getTimestamp().isAfter(crossingTime))
                .max(Comparator.comparing(TollTransaction::getTimestamp));

        if (lastTrip.isPresent()) {
            TollTransaction prev = lastTrip.get();
            if (prev.getTripType() == TripType.SINGLE) {
                long hours = Duration.between(prev.getTimestamp(), crossingTime).toHours();
                return hours >= 0 && hours < 24;
            }
        }
        return false;
    }

    @Override
    public boolean isCooldownActive(String tagId, int plazaId, LocalDateTime currentTime) {
        if (tagId == null) return false;
        LocalDateTime lastTime = lastChargedMap.get(cooldownKey(tagId, plazaId));
        if (lastTime == null) return false;

        long elapsedSecs = Duration.between(lastTime, currentTime).getSeconds();
        return elapsedSecs >= 0 && elapsedSecs < 120; // 2 minutes
    }

    @Override
    public long getRemainingCooldownSeconds(String tagId, int plazaId, LocalDateTime currentTime) {
        if (tagId == null) return 0;
        LocalDateTime lastTime = lastChargedMap.get(cooldownKey(tagId, plazaId));
        if (lastTime == null) return 0;

        long elapsedSecs = Duration.between(lastTime, currentTime).getSeconds();
        if (elapsedSecs >= 0 && elapsedSecs < 120) {
            return 120 - elapsedSecs;
        }
        return 0;
    }

    private double getBaseRate(int plazaId, VehicleType type) {
        Double rate = ratesMap.get(rateKey(plazaId, type));
        if (rate != null) return rate;
        switch (type) {
            case CAR: return 80.0;
            case VAN: return 130.0;
            case BUS:
            case TRUCK: return 270.0;
            case MULTI_AXLE: return 420.0;
            default: return 80.0;
        }
    }

    @Override
    public List<TollTransaction> getAllTransactions() {
        return Collections.unmodifiableList(transactionHistory);
    }

    @Override
    public List<TollTransaction> getFlaggedTransactions() {
        return transactionHistory.stream()
                .filter(TollTransaction::isFlaggedForReview)
                .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
    }

    @Override
    public void registerBaseRate(int plazaId, VehicleType vehicleType, double singleRate) {
        ratesMap.put(rateKey(plazaId, vehicleType), singleRate);
    }

    @Override
    public void registerMonthlyPass(String vehicleRegNo, int plazaId, boolean active) {
        passMap.put(passKey(vehicleRegNo, plazaId), active);
    }

    @Override
    public void setLaneOpen(int plazaId, int laneNumber, boolean isOpen) {
        laneStatusMap.put(laneKey(plazaId, laneNumber), isOpen);
    }
}
