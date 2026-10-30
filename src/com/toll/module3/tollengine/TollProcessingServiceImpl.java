package com.toll.module3.tollengine;

import com.toll.common.AccountStatus;
import com.toll.common.DataSeeder;
import com.toll.common.VehicleType;
import com.toll.module1.plaza.IPlazaService;
import com.toll.module1.plaza.model.Lane;
import com.toll.module1.plaza.model.TollPlaza;
import com.toll.module2.account.model.FASTagAccount;
import com.toll.module2.account.model.Vehicle;
import com.toll.module2.account.repository.InMemoryFASTagAccountRepository;
import com.toll.module2.account.repository.InMemoryVehicleRepository;
import com.toll.module2.account.service.AccountServiceImpl;
import com.toll.module2.account.service.DebitResult;
import com.toll.module2.account.service.IAccountService;
import com.toll.module3.tollengine.repository.ITransactionRepository;
import com.toll.module4.pricing.IPricingService;
import com.toll.module4.pricing.PricingResult;
import com.toll.module5.billing.IBillingReportingService;
import com.toll.module5.billing.payment.CashPaymentMethod;
import com.toll.module5.billing.payment.FASTagPaymentMethod;
import com.toll.module5.billing.payment.PaymentContext;
import com.toll.module5.billing.payment.PaymentResult;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Unified Toll Processing Engine implementing ITollProcessingService.
 * Supports standalone Module 3 operation as well as full 5-Module integrated cross-cutting pipelines.
 */
public class TollProcessingServiceImpl implements ITollProcessingService {

    // --- Standalone Module 3 Fields ---
    private final IAccountService accountService;
    private final Map<String, BarrierGate> barrierMap = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lastChargedMap = new ConcurrentHashMap<>();
    private final Map<String, Double> ratesMap = new ConcurrentHashMap<>();
    private final Map<String, Boolean> passMap = new ConcurrentHashMap<>();
    private final Map<String, Boolean> laneStatusMap = new ConcurrentHashMap<>();
    private final List<TollTransaction> transactionHistory = new CopyOnWriteArrayList<>();

    // --- 5-Module Integrated Dependencies (Optional in standalone mode) ---
    private IPlazaService plazaService;
    private IPricingService pricingService;
    private IBillingReportingService billingService;
    private ITransactionRepository transactionRepository;
    private FASTagPaymentMethod fastagPaymentMethod;
    private CashPaymentMethod cashPaymentMethod;

    /**
     * Default No-Arg Constructor (Used by Module3CLI).
     */
    public TollProcessingServiceImpl() {
        this(new AccountServiceImpl(new InMemoryVehicleRepository(), new InMemoryFASTagAccountRepository()));
        DataSeeder.seedModule2Data(this.accountService);
    }

    /**
     * Standalone Constructor (Used by Module 3 CLI & Module3TestSuite).
     */
    public TollProcessingServiceImpl(IAccountService accountService) {
        if (accountService == null) {
            throw new IllegalArgumentException("AccountService cannot be null.");
        }
        this.accountService = accountService;
        initDefaultRates();
    }

    /**
     * Full 5-Module Integrated Constructor (Used by MainCLI & AllModulesTestSuite).
     */
    public TollProcessingServiceImpl(IPlazaService plazaService,
                                     IAccountService accountService,
                                     IPricingService pricingService,
                                     IBillingReportingService billingService,
                                     ITransactionRepository transactionRepository) {
        this.plazaService = plazaService;
        this.accountService = accountService;
        this.pricingService = pricingService;
        this.billingService = billingService;
        this.transactionRepository = transactionRepository;

        this.fastagPaymentMethod = new FASTagPaymentMethod(accountService);
        this.cashPaymentMethod = new CashPaymentMethod();
        initDefaultRates();
    }

    private void initDefaultRates() {
        registerBaseRate(1, VehicleType.CAR, 80.0);
        registerBaseRate(1, VehicleType.VAN, 130.0);
        registerBaseRate(1, VehicleType.BUS, 270.0);
        registerBaseRate(1, VehicleType.TRUCK, 270.0);
        registerBaseRate(1, VehicleType.MULTI_AXLE, 420.0);

        registerBaseRate(2, VehicleType.CAR, 100.0);
        registerBaseRate(2, VehicleType.VAN, 160.0);
        registerBaseRate(2, VehicleType.BUS, 330.0);
        registerBaseRate(2, VehicleType.TRUCK, 330.0);
        registerBaseRate(2, VehicleType.MULTI_AXLE, 520.0);
    }

    private String rateKey(int plazaId, VehicleType type) {
        return plazaId + ":" + type.name();
    }

    private String passKey(String vehicleRegNo, int plazaId) {
        return vehicleRegNo.trim().toUpperCase() + ":" + plazaId;
    }

    private String laneKey(int plazaId, int laneNumber) {
        return plazaId + ":" + laneNumber;
    }

    private String cooldownKey(String tagId, int plazaId) {
        return tagId.trim().toUpperCase() + ":" + plazaId;
    }

    @Override
    public BarrierGate getBarrier(int plazaId, int laneNumber) {
        return barrierMap.computeIfAbsent(laneKey(plazaId, laneNumber), k -> new BarrierGate(plazaId, laneNumber));
    }

    public static double roundToNearest5(double amount) {
        return Math.round(amount / 5.0) * 5.0;
    }

    // --- Standalone Crossing Implementations ---

    @Override
    public CrossingResult processCrossing(int plazaId, int laneNumber, String tagId, VehicleType observedType) {
        return processCrossing(plazaId, laneNumber, tagId, observedType, LocalDateTime.now());
    }

    @Override
    public CrossingResult processCrossing(int plazaId, int laneNumber, String tagId, VehicleType observedType, LocalDateTime crossingTime) {
        BarrierGate barrier = getBarrier(plazaId, laneNumber);
        String txnId = UUID.randomUUID().toString();

        if (Boolean.FALSE.equals(laneStatusMap.get(laneKey(plazaId, laneNumber)))) {
            barrier.closeBarrier();
            return new CrossingResult(false, txnId, 0.0, "FAILED",
                    "Lane #" + laneNumber + " at Plaza #" + plazaId + " is currently CLOSED.", false);
        }

        if (tagId == null || tagId.trim().isEmpty()) {
            barrier.closeBarrier();
            VehicleType type = (observedType != null) ? observedType : VehicleType.CAR;
            double doubleToll = 2.0 * getBaseRate(plazaId, type);
            TollTransaction tx = TollTransaction.builder()
                    .transactionId(txnId)
                    .plazaId(plazaId)
                    .laneNumber(laneNumber)
                    .vehicleRegNo("UNKNOWN")
                    .timestamp(crossingTime)
                    .tollAmount(doubleToll)
                    .observedType(observedType)
                    .billedType(observedType)
                    .tripType(TripType.SINGLE)
                    .paymentMethod(PaymentMethod.CASH)
                    .status("FAILED")
                    .failureReason("No valid active tag detected in FASTag lane. Diverted to Cash lane for double toll penalty.")
                    .build();
            transactionHistory.add(tx);

            return new CrossingResult(false, txnId, doubleToll, "FAILED",
                    "No valid active tag detected in FASTag lane! Barrier closed. Diverted to Cash Lane (Double Toll penalty: Rs. " + doubleToll + ").", false);
        }

        String formattedTag = tagId.trim().toUpperCase();
        Optional<FASTagAccount> accountOpt = accountService.findAccountByTagId(formattedTag);

        if (accountOpt.isEmpty() || accountOpt.get().getStatus() == AccountStatus.CLOSED) {
            barrier.closeBarrier();
            VehicleType type = (observedType != null) ? observedType : VehicleType.CAR;
            double doubleToll = 2.0 * getBaseRate(plazaId, type);
            TollTransaction tx = TollTransaction.builder()
                    .transactionId(txnId)
                    .tagId(formattedTag)
                    .plazaId(plazaId)
                    .laneNumber(laneNumber)
                    .vehicleRegNo("UNREGISTERED")
                    .timestamp(crossingTime)
                    .tollAmount(doubleToll)
                    .observedType(observedType)
                    .billedType(observedType)
                    .tripType(TripType.SINGLE)
                    .paymentMethod(PaymentMethod.CASH)
                    .status("FAILED")
                    .failureReason("Tag not found or CLOSED. Diverted to Cash lane.")
                    .build();
            transactionHistory.add(tx);

            return new CrossingResult(false, txnId, doubleToll, "FAILED",
                    "No valid active tag detected in FASTag lane! Barrier closed. Diverted to Cash Lane (Double Toll penalty: Rs. " + doubleToll + ").", false);
        }

        FASTagAccount account = accountOpt.get();
        String vehicleRegNo = account.getVehicleRegistrationNumber();
        Optional<Vehicle> vehicleOpt = accountService.findVehicleByRegistration(vehicleRegNo);
        VehicleType registeredType = vehicleOpt.map(Vehicle::getVehicleType).orElse(VehicleType.CAR);

        if (account.isBlacklisted()) {
            barrier.closeBarrier();
            TollTransaction tx = TollTransaction.builder()
                    .transactionId(txnId)
                    .tagId(formattedTag)
                    .plazaId(plazaId)
                    .laneNumber(laneNumber)
                    .vehicleRegNo(vehicleRegNo)
                    .timestamp(crossingTime)
                    .tollAmount(0.0)
                    .observedType(observedType)
                    .billedType(registeredType)
                    .tripType(TripType.SINGLE)
                    .paymentMethod(PaymentMethod.FASTAG)
                    .status("FAILED")
                    .flaggedForReview(true)
                    .failureReason("Blacklisted tag detected: " + account.getBlacklistReason())
                    .build();
            transactionHistory.add(tx);

            return new CrossingResult(false, txnId, 0.0, "FAILED",
                    "Crossing Refused! FASTag [" + formattedTag + "] is BLACKLISTED (" + account.getBlacklistReason() + "). Barrier stays closed.", true);
        }

        if (isCooldownActive(formattedTag, plazaId, crossingTime)) {
            barrier.closeBarrier();
            long remaining = getRemainingCooldownSeconds(formattedTag, plazaId, crossingTime);
            TollTransaction tx = TollTransaction.builder()
                    .transactionId(txnId)
                    .tagId(formattedTag)
                    .plazaId(plazaId)
                    .laneNumber(laneNumber)
                    .vehicleRegNo(vehicleRegNo)
                    .timestamp(crossingTime)
                    .tollAmount(0.0)
                    .observedType(observedType)
                    .billedType(registeredType)
                    .tripType(TripType.SINGLE)
                    .paymentMethod(PaymentMethod.FASTAG)
                    .status("FAILED")
                    .failureReason("Anti-double-billing protection: Charged within 2 minutes")
                    .build();
            transactionHistory.add(tx);

            return new CrossingResult(false, txnId, 0.0, "FAILED",
                    "Duplicate crossing rejected: Anti-Double-Billing Active for tag [" + formattedTag + "] at Plaza #" + plazaId + " within 2 minutes. Remaining cooldown: " + remaining + "s.", false);
        }

        boolean hasMonthlyPass = Boolean.TRUE.equals(passMap.get(passKey(vehicleRegNo, plazaId)));
        if (hasMonthlyPass) {
            barrier.openBarrier();
            lastChargedMap.put(cooldownKey(formattedTag, plazaId), crossingTime);

            TollTransaction tx = TollTransaction.builder()
                    .transactionId(txnId)
                    .tagId(formattedTag)
                    .plazaId(plazaId)
                    .laneNumber(laneNumber)
                    .vehicleRegNo(vehicleRegNo)
                    .timestamp(crossingTime)
                    .tollAmount(0.0)
                    .observedType(observedType)
                    .billedType(registeredType)
                    .tripType(TripType.PASS)
                    .paymentMethod(PaymentMethod.FASTAG)
                    .status("SUCCESS")
                    .build();
            transactionHistory.add(tx);

            return new CrossingResult(true, txnId, 0.0, "SUCCESS",
                    "Monthly Pass Applied: Vehicle [" + vehicleRegNo + "] cleared plaza #" + plazaId + " at Rs. 0.00. Barrier OPEN.", false);
        }

        double registeredRate = getBaseRate(plazaId, registeredType);
        double observedRate = (observedType != null) ? getBaseRate(plazaId, observedType) : registeredRate;

        boolean isMismatch = (observedType != null && observedType != registeredType);
        double baseCalculatedToll = isMismatch ? Math.max(registeredRate, observedRate) : registeredRate;
        VehicleType billedType = (isMismatch && observedRate > registeredRate) ? observedType : registeredType;

        boolean isReturnTrip = isReturnTripWithin24Hours(vehicleRegNo, plazaId, crossingTime);
        TripType tripType = isReturnTrip ? TripType.RETURN : TripType.SINGLE;
        if (isReturnTrip) {
            baseCalculatedToll = baseCalculatedToll * 0.50;
        }

        double finalToll = roundToNearest5(baseCalculatedToll);

        if (account.getStatus() == AccountStatus.BLOCKED || account.getBalance() < finalToll) {
            barrier.closeBarrier();
            TollTransaction tx = TollTransaction.builder()
                    .transactionId(txnId)
                    .tagId(formattedTag)
                    .plazaId(plazaId)
                    .laneNumber(laneNumber)
                    .vehicleRegNo(vehicleRegNo)
                    .timestamp(crossingTime)
                    .tollAmount(finalToll)
                    .observedType(observedType)
                    .billedType(billedType)
                    .tripType(tripType)
                    .paymentMethod(PaymentMethod.FASTAG)
                    .status("FAILED")
                    .flaggedForReview(isMismatch)
                    .failureReason("Tag balance insufficient or account BLOCKED. Required: Rs. " + finalToll + ", Available: Rs. " + account.getBalance())
                    .build();
            transactionHistory.add(tx);

            return new CrossingResult(false, txnId, finalToll, "FAILED",
                    "Tag balance insufficient (Available: Rs. " + account.getBalance() + ", Required: Rs. " + finalToll + ")! Barrier closed. Diverted to Cash Lane (Pay normal toll).", isMismatch);
        }

        DebitResult debitResult = accountService.deductToll(formattedTag, finalToll);
        if (!debitResult.isSuccessful()) {
            barrier.closeBarrier();
            return new CrossingResult(false, txnId, finalToll, "FAILED",
                    "Deduction failed: " + debitResult.getMessage() + ". Diverted to Cash Lane.", isMismatch);
        }

        barrier.openBarrier();
        lastChargedMap.put(cooldownKey(formattedTag, plazaId), crossingTime);

        TollTransaction tx = TollTransaction.builder()
                .transactionId(txnId)
                .tagId(formattedTag)
                .plazaId(plazaId)
                .laneNumber(laneNumber)
                .vehicleRegNo(vehicleRegNo)
                .timestamp(crossingTime)
                .tollAmount(finalToll)
                .observedType(observedType)
                .billedType(billedType)
                .tripType(tripType)
                .paymentMethod(PaymentMethod.FASTAG)
                .status("SUCCESS")
                .flaggedForReview(isMismatch)
                .build();
        transactionHistory.add(tx);

        String successMsg = String.format("FASTag Toll Deducted: Rs. %.2f (%s%s). New Balance: Rs. %.2f. Barrier OPEN.",
                finalToll, tripType.getDescription(),
                isMismatch ? " | AVC Mismatch: higher rate charged" : "",
                debitResult.getRemainingBalance());

        return new CrossingResult(true, txnId, finalToll, "SUCCESS", successMsg, isMismatch);
    }

    @Override
    public CrossingResult processEmergencyCrossing(int plazaId, int laneNumber, String vehicleRegNo) {
        return processEmergencyCrossing(plazaId, laneNumber, vehicleRegNo, LocalDateTime.now());
    }

    @Override
    public CrossingResult processEmergencyCrossing(int plazaId, int laneNumber, String vehicleRegNo, LocalDateTime crossingTime) {
        BarrierGate barrier = getBarrier(plazaId, laneNumber);
        String txnId = UUID.randomUUID().toString();
        String reg = (vehicleRegNo != null && !vehicleRegNo.trim().isEmpty()) ? vehicleRegNo.trim().toUpperCase() : "EMERGENCY-UNKNOWN";

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
    public CrossingResult processEmergencyCrossing(int plazaId, int laneNumber, String vehicleRegNo, String emergencyType, LocalDateTime crossingTime) {
        return processEmergencyCrossing(plazaId, laneNumber, vehicleRegNo, crossingTime);
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
        return elapsedSecs >= 0 && elapsedSecs < 120;
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

    // --- 5-Module Integrated Pipeline Method ---

    @Override
    public CrossingResponse processFASTagCrossing(int plazaId, int laneNumber, String tagId,
                                                 VehicleType observedType, LocalDateTime crossingTime) {
        String txnId = UUID.randomUUID().toString();
        TollPlaza plaza = plazaService.getPlaza(plazaId)
                .orElseThrow(() -> new IllegalArgumentException("Plaza #" + plazaId + " not found."));

        if (plaza.getStatus() != com.toll.common.PlazaStatus.ACTIVE) {
            com.toll.module3.tollengine.model.TollTransaction txn = new com.toll.module3.tollengine.model.TollTransaction(
                    txnId, "UNKNOWN", plazaId, laneNumber, tagId, crossingTime,
                    0.0, observedType, null, com.toll.common.TripType.SINGLE, com.toll.common.PaymentType.FASTAG,
                    com.toll.common.TransactionStatus.FAILED, false, null, "Plaza is not OPEN/ACTIVE.");
            if (transactionRepository != null) transactionRepository.save(txn);
            return new CrossingResponse(false, txn, "Plaza is currently CLOSED.", false, false);
        }

        Lane lane = plaza.findLane(laneNumber)
                .orElseThrow(() -> new IllegalArgumentException("Lane #" + laneNumber + " not found in Plaza #" + plazaId));

        if (lane.getStatus() != com.toll.common.LaneStatus.OPEN) {
            com.toll.module3.tollengine.model.TollTransaction txn = new com.toll.module3.tollengine.model.TollTransaction(
                    txnId, "UNKNOWN", plazaId, laneNumber, tagId, crossingTime,
                    0.0, observedType, null, com.toll.common.TripType.SINGLE, com.toll.common.PaymentType.FASTAG,
                    com.toll.common.TransactionStatus.FAILED, false, null, "Lane #" + laneNumber + " is CLOSED.");
            if (transactionRepository != null) transactionRepository.save(txn);
            return new CrossingResponse(false, txn, "Lane #" + laneNumber + " is CLOSED.", false, false);
        }

        lane.setProcessingVehicle(true);
        try {
            Optional<FASTagAccount> accountOpt = accountService.findAccountByTagId(tagId);
            if (accountOpt.isEmpty() || accountOpt.get().getStatus() == AccountStatus.CLOSED) {
                com.toll.module3.tollengine.model.TollTransaction failedTxn = new com.toll.module3.tollengine.model.TollTransaction(
                        txnId, "UNKNOWN", plazaId, laneNumber, tagId, crossingTime,
                        0.0, observedType, observedType, com.toll.common.TripType.SINGLE, com.toll.common.PaymentType.FASTAG,
                        com.toll.common.TransactionStatus.FAILED, false, null,
                        "Vehicle without valid active FASTag. Divert to Cash Lane (Double Toll penalty).");
                if (transactionRepository != null) transactionRepository.save(failedTxn);
                return new CrossingResponse(false, failedTxn,
                        "No valid active FASTag detected! Barrier CLOSED -> Diverted to Cash Lane (DOUBLE TOLL PENALTY).",
                        true, true);
            }

            FASTagAccount account = accountOpt.get();
            String vehicleRegNo = account.getVehicleRegistrationNumber();
            Vehicle vehicle = accountService.findVehicleByRegistration(vehicleRegNo)
                    .orElseThrow(() -> new IllegalStateException("Registered vehicle not found for tag: " + tagId));
            VehicleType registeredType = vehicle.getVehicleType();

            if (account.isBlacklisted()) {
                com.toll.module3.tollengine.model.TollTransaction failedTxn = new com.toll.module3.tollengine.model.TollTransaction(
                        txnId, vehicleRegNo, plazaId, laneNumber, tagId, crossingTime,
                        0.0, observedType, registeredType, com.toll.common.TripType.SINGLE, com.toll.common.PaymentType.FASTAG,
                        com.toll.common.TransactionStatus.FAILED, true, null,
                        "REFUSED: Tag is BLACKLISTED (" + account.getBlacklistReason() + ")");
                if (transactionRepository != null) transactionRepository.save(failedTxn);
                return new CrossingResponse(false, failedTxn,
                        "ALARM: Tag " + tagId + " is BLACKLISTED! Refused at toll plaza. Barrier CLOSED.", false, false);
            }

            boolean duplicateAttempt = (transactionRepository != null) && transactionRepository.findByTagId(tagId).stream()
                    .anyMatch(t -> t.plazaId() == plazaId &&
                                   t.isSuccessful() &&
                                   t.paymentMethod() == com.toll.common.PaymentType.FASTAG &&
                                   Math.abs(ChronoUnit.SECONDS.between(t.timestamp(), crossingTime)) < 120);

            if (duplicateAttempt) {
                com.toll.module3.tollengine.model.TollTransaction failedTxn = new com.toll.module3.tollengine.model.TollTransaction(
                        txnId, vehicleRegNo, plazaId, laneNumber, tagId, crossingTime,
                        0.0, observedType, registeredType, com.toll.common.TripType.SINGLE, com.toll.common.PaymentType.FASTAG,
                        com.toll.common.TransactionStatus.FAILED, false, null,
                        "REJECTED: Anti-double-billing cooldown (Same tag charged within 2 minutes at this plaza).");
                transactionRepository.save(failedTxn);
                return new CrossingResponse(false, failedTxn,
                        "REJECTED: Tag charged at this plaza within 2 minutes. Barrier CLOSED.", false, false);
            }

            boolean hadSingleTripWithin24h = (transactionRepository != null) && transactionRepository.findByVehicle(vehicleRegNo).stream()
                    .anyMatch(t -> t.plazaId() == plazaId &&
                                   t.isSuccessful() &&
                                   t.tripType() == com.toll.common.TripType.SINGLE &&
                                   Math.abs(ChronoUnit.HOURS.between(t.timestamp(), crossingTime)) <= 24);

            PricingResult pricing = pricingService.calculateFare(plazaId, vehicleRegNo, registeredType,
                    observedType, crossingTime, hadSingleTripWithin24h, plazaService);

            VehicleType billedType = (observedType != null && observedType != registeredType && pricing.higherTollCharged())
                    ? observedType : registeredType;
            boolean flaggedForReview = (observedType != null && observedType != registeredType);

            if (pricing.passApplied()) {
                com.toll.module3.tollengine.model.TollTransaction passTxn = new com.toll.module3.tollengine.model.TollTransaction(
                        txnId, vehicleRegNo, plazaId, laneNumber, tagId, crossingTime,
                        0.0, observedType, billedType, com.toll.common.TripType.PASS, com.toll.common.PaymentType.FASTAG,
                        com.toll.common.TransactionStatus.SUCCESS, flaggedForReview, null, null);
                if (transactionRepository != null) transactionRepository.save(passTxn);
                return new CrossingResponse(true, passTxn,
                        "Active Monthly Pass honored. Toll = Rs. 0.00. Boom barrier OPENED.", false, false);
            }

            PaymentContext payCtx = new PaymentContext(tagId, vehicleRegNo, plazaId, laneNumber, pricing.roundedFinalFare(), null);
            PaymentResult payResult = (fastagPaymentMethod != null) ? fastagPaymentMethod.processPayment(payCtx)
                    : PaymentResult.failure(com.toll.common.PaymentType.FASTAG, pricing.roundedFinalFare(), "No payment method configured");

            if (payResult.successful()) {
                if (payResult.lowBalanceAlert() && billingService != null) {
                    billingService.triggerLowBalanceAlert(tagId, vehicleRegNo, account.getBalance());
                }

                com.toll.module3.tollengine.model.TollTransaction successTxn = new com.toll.module3.tollengine.model.TollTransaction(
                        txnId, vehicleRegNo, plazaId, laneNumber, tagId, crossingTime,
                        pricing.roundedFinalFare(), observedType, billedType, pricing.tripType(),
                        com.toll.common.PaymentType.FASTAG, com.toll.common.TransactionStatus.SUCCESS, flaggedForReview, null, null);
                if (transactionRepository != null) transactionRepository.save(successTxn);

                String reviewMsg = flaggedForReview ? " [AVC MISMATCH: Higher toll charged & flagged for review]" : "";
                String blockMsg = payResult.autoBlocked() ? " [ACCOUNT AUTO-BLOCKED: Balance < Rs. 100]" : "";
                String alertMsg = payResult.lowBalanceAlert() ? " [LOW BALANCE ALERT: Balance < Rs. 200]" : "";

                return new CrossingResponse(true, successTxn,
                        String.format("Toll Rs. %.2f deducted successfully. Barrier OPENED.%s%s%s",
                                pricing.roundedFinalFare(), reviewMsg, blockMsg, alertMsg),
                        false, false);
            } else {
                com.toll.module3.tollengine.model.TollTransaction failedTxn = new com.toll.module3.tollengine.model.TollTransaction(
                        txnId, vehicleRegNo, plazaId, laneNumber, tagId, crossingTime,
                        pricing.roundedFinalFare(), observedType, billedType, pricing.tripType(),
                        com.toll.common.PaymentType.FASTAG, com.toll.common.TransactionStatus.FAILED, flaggedForReview, null, payResult.message());
                if (transactionRepository != null) transactionRepository.save(failedTxn);

                return new CrossingResponse(false, failedTxn,
                        "FASTag deduction failed (" + payResult.message() + "). Barrier CLOSED -> Diverted to Cash Lane (Pay NORMAL toll).",
                        true, false);
            }
        } finally {
            lane.setProcessingVehicle(false);
        }
    }
}
