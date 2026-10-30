package com.toll.module3.tollengine;

import com.toll.common.*;
import com.toll.module1.plaza.IPlazaService;
import com.toll.module1.plaza.model.Lane;
import com.toll.module1.plaza.model.TollPlaza;
import com.toll.module2.account.model.FASTagAccount;
import com.toll.module2.account.model.Vehicle;
import com.toll.module2.account.service.IAccountService;
import com.toll.module3.tollengine.model.TollTransaction;
import com.toll.module3.tollengine.repository.ITransactionRepository;
import com.toll.module4.pricing.IPricingService;
import com.toll.module4.pricing.PricingResult;
import com.toll.module5.billing.IBillingReportingService;
import com.toll.module5.billing.payment.CashPaymentMethod;
import com.toll.module5.billing.payment.FASTagPaymentMethod;
import com.toll.module5.billing.payment.PaymentContext;
import com.toll.module5.billing.payment.PaymentResult;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Core Toll Processing Engine implementing Module 3.
 * Orchestrates Plaza/Lane safety, FASTag deductions, AVC mismatch flagging,
 * 24h return discounting, 2-minute duplicate suppression, cash fallbacks, and emergency bypass.
 */
public class TollProcessingServiceImpl implements ITollProcessingService {
    private final IPlazaService plazaService;
    private final IAccountService accountService;
    private final IPricingService pricingService;
    private final IBillingReportingService billingService;
    private final ITransactionRepository transactionRepository;

    private final FASTagPaymentMethod fastagPaymentMethod;
    private final CashPaymentMethod cashPaymentMethod;

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
    }

    @Override
    public CrossingResponse processFASTagCrossing(int plazaId, int laneNumber, String tagId,
                                                 VehicleType observedType, LocalDateTime crossingTime) {
        String txnId = UUID.randomUUID().toString();
        TollPlaza plaza = plazaService.getPlaza(plazaId)
                .orElseThrow(() -> new IllegalArgumentException("Plaza #" + plazaId + " not found."));

        if (plaza.getStatus() != PlazaStatus.ACTIVE) {
            TollTransaction txn = new TollTransaction(txnId, "UNKNOWN", plazaId, laneNumber, tagId, crossingTime,
                    0.0, observedType, null, TripType.SINGLE, PaymentType.FASTAG, TransactionStatus.FAILED,
                    false, null, "Plaza is not OPEN/ACTIVE.");
            transactionRepository.save(txn);
            return new CrossingResponse(false, txn, "Plaza is currently CLOSED.", false, false);
        }

        Lane lane = plaza.findLane(laneNumber)
                .orElseThrow(() -> new IllegalArgumentException("Lane #" + laneNumber + " not found in Plaza #" + plazaId));

        if (lane.getStatus() != LaneStatus.OPEN) {
            TollTransaction txn = new TollTransaction(txnId, "UNKNOWN", plazaId, laneNumber, tagId, crossingTime,
                    0.0, observedType, null, TripType.SINGLE, PaymentType.FASTAG, TransactionStatus.FAILED,
                    false, null, "Lane #" + laneNumber + " is CLOSED.");
            transactionRepository.save(txn);
            return new CrossingResponse(false, txn, "Lane #" + laneNumber + " is CLOSED.", false, false);
        }

        // Set lane processing state for safety closing rule
        lane.setProcessingVehicle(true);
        try {
            // 1. Tag & Account lookup
            Optional<FASTagAccount> accountOpt = accountService.findAccountByTagId(tagId);
            if (accountOpt.isEmpty() || accountOpt.get().getStatus() == AccountStatus.CLOSED) {
                // Rule: "A vehicle without a valid Active tag is sent to a Cash lane and pays double the toll."
                TollTransaction failedTxn = new TollTransaction(txnId, "UNKNOWN", plazaId, laneNumber, tagId, crossingTime,
                        0.0, observedType, observedType, TripType.SINGLE, PaymentType.FASTAG, TransactionStatus.FAILED,
                        false, null, "Vehicle without valid active FASTag. Divert to Cash Lane (Double Toll penalty).");
                transactionRepository.save(failedTxn);
                return new CrossingResponse(false, failedTxn,
                        "No valid active FASTag detected! Barrier CLOSED -> Diverted to Cash Lane (DOUBLE TOLL PENALTY).",
                        true, true);
            }

            FASTagAccount account = accountOpt.get();
            String vehicleRegNo = account.getVehicleRegistrationNumber();
            Vehicle vehicle = accountService.findVehicleByRegistration(vehicleRegNo)
                    .orElseThrow(() -> new IllegalStateException("Registered vehicle not found for tag: " + tagId));
            VehicleType registeredType = vehicle.getVehicleType();

            // 2. Blacklist check
            // Rule: "An Admin can blacklist a tag (for example, a stolen or fraudulent tag). A blacklisted tag is refused at every plaza."
            if (account.isBlacklisted()) {
                TollTransaction failedTxn = new TollTransaction(txnId, vehicleRegNo, plazaId, laneNumber, tagId, crossingTime,
                        0.0, observedType, registeredType, TripType.SINGLE, PaymentType.FASTAG, TransactionStatus.FAILED,
                        true, null, "REFUSED: Tag is BLACKLISTED (" + account.getBlacklistReason() + ")");
                transactionRepository.save(failedTxn);
                return new CrossingResponse(false, failedTxn,
                        "ALARM: Tag " + tagId + " is BLACKLISTED! Refused at toll plaza. Barrier CLOSED.", false, false);
            }

            // 3. 2-Minute Anti-Double-Billing check
            // Rule: "The same tag cannot be charged twice at the same plaza within 2 minutes."
            boolean duplicateAttempt = transactionRepository.findByTagId(tagId).stream()
                    .anyMatch(t -> t.plazaId() == plazaId &&
                                   t.isSuccessful() &&
                                   t.paymentMethod() == PaymentType.FASTAG &&
                                   Math.abs(ChronoUnit.SECONDS.between(t.timestamp(), crossingTime)) < 120);

            if (duplicateAttempt) {
                TollTransaction failedTxn = new TollTransaction(txnId, vehicleRegNo, plazaId, laneNumber, tagId, crossingTime,
                        0.0, observedType, registeredType, TripType.SINGLE, PaymentType.FASTAG, TransactionStatus.FAILED,
                        false, null, "REJECTED: Anti-double-billing cooldown (Same tag charged within 2 minutes at this plaza).");
                transactionRepository.save(failedTxn);
                return new CrossingResponse(false, failedTxn,
                        "REJECTED: Tag charged at this plaza within 2 minutes. Barrier CLOSED.", false, false);
            }

            // 4. 24-Hour Return trip check (50% discount on Single trip)
            boolean hadSingleTripWithin24h = transactionRepository.findByVehicle(vehicleRegNo).stream()
                    .anyMatch(t -> t.plazaId() == plazaId &&
                                   t.isSuccessful() &&
                                   t.tripType() == TripType.SINGLE &&
                                   Math.abs(ChronoUnit.HOURS.between(t.timestamp(), crossingTime)) <= 24);

            // 5. Pricing calculation (Pass, Return 50% discount, AVC mismatch higher toll, Round to nearest Rs. 5)
            PricingResult pricing = pricingService.calculateFare(plazaId, vehicleRegNo, registeredType,
                    observedType, crossingTime, hadSingleTripWithin24h, plazaService);

            VehicleType billedType = (observedType != null && observedType != registeredType && pricing.higherTollCharged())
                    ? observedType : registeredType;
            boolean flaggedForReview = (observedType != null && observedType != registeredType);

            // 6. If Monthly Pass (Toll = Rs. 0)
            if (pricing.passApplied()) {
                TollTransaction passTxn = new TollTransaction(txnId, vehicleRegNo, plazaId, laneNumber, tagId, crossingTime,
                        0.0, observedType, billedType, TripType.PASS, PaymentType.FASTAG, TransactionStatus.SUCCESS,
                        flaggedForReview, null, null);
                transactionRepository.save(passTxn);
                return new CrossingResponse(true, passTxn,
                        "Active Monthly Pass honored. Toll = Rs. 0.00. Boom barrier OPENED.", false, false);
            }

            // 7. Debit Toll from FASTag
            PaymentContext payCtx = new PaymentContext(tagId, vehicleRegNo, plazaId, laneNumber, pricing.roundedFinalFare(), null);
            PaymentResult payResult = fastagPaymentMethod.processPayment(payCtx);

            if (payResult.successful()) {
                // Low balance alert trigger (< Rs. 200)
                if (payResult.lowBalanceAlert()) {
                    billingService.triggerLowBalanceAlert(tagId, vehicleRegNo, account.getBalance());
                }

                TollTransaction successTxn = new TollTransaction(txnId, vehicleRegNo, plazaId, laneNumber, tagId, crossingTime,
                        pricing.roundedFinalFare(), observedType, billedType, pricing.tripType(),
                        PaymentType.FASTAG, TransactionStatus.SUCCESS, flaggedForReview, null, null);
                transactionRepository.save(successTxn);

                String reviewMsg = flaggedForReview ? " [AVC MISMATCH: Higher toll charged & flagged for review]" : "";
                String blockMsg = payResult.autoBlocked() ? " [ACCOUNT AUTO-BLOCKED: Balance < Rs. 100]" : "";
                String alertMsg = payResult.lowBalanceAlert() ? " [LOW BALANCE ALERT: Balance < Rs. 200]" : "";

                return new CrossingResponse(true, successTxn,
                        String.format("Toll Rs. %.2f deducted successfully. Barrier OPENED.%s%s%s",
                                pricing.roundedFinalFare(), reviewMsg, blockMsg, alertMsg),
                        false, false);
            } else {
                // Rule: "If the tag balance is not enough, the barrier does not open. The vehicle is sent to a Cash lane and pays the normal toll in cash."
                TollTransaction failedTxn = new TollTransaction(txnId, vehicleRegNo, plazaId, laneNumber, tagId, crossingTime,
                        pricing.roundedFinalFare(), observedType, billedType, pricing.tripType(),
                        PaymentType.FASTAG, TransactionStatus.FAILED, flaggedForReview, null, payResult.message());
                transactionRepository.save(failedTxn);

                return new CrossingResponse(false, failedTxn,
                        "FASTag deduction failed (" + payResult.message() + "). Barrier CLOSED -> Diverted to Cash Lane (Pay NORMAL toll).",
                        true, false);
            }
        } finally {
            lane.setProcessingVehicle(false);
        }
    }

    @Override
    public CrossingResponse processCashCrossing(int plazaId, int laneNumber, String vehicleRegNo,
                                               VehicleType vehicleType, String operatorName,
                                               boolean isDoubleTollPenalty, LocalDateTime crossingTime) {
        String txnId = UUID.randomUUID().toString();
        TollPlaza plaza = plazaService.getPlaza(plazaId)
                .orElseThrow(() -> new IllegalArgumentException("Plaza #" + plazaId + " not found."));

        Lane lane = plaza.findLane(laneNumber)
                .orElseThrow(() -> new IllegalArgumentException("Lane #" + laneNumber + " not found in Plaza #" + plazaId));

        lane.setProcessingVehicle(true);
        try {
            double singleRate = plazaService.getActiveRate(plazaId, vehicleType, TripType.SINGLE, crossingTime.toLocalDate());
            // Double toll penalty for vehicles without active tag in FASTag lane
            double baseAmount = isDoubleTollPenalty ? (singleRate * 2.0) : singleRate;
            double payableToll = pricingService.roundToNearestFive(baseAmount);

            PaymentContext payCtx = new PaymentContext(null, vehicleRegNo, plazaId, laneNumber, payableToll, operatorName);
            PaymentResult payResult = cashPaymentMethod.processPayment(payCtx);

            billingService.recordCashPayment(txnId, plazaId, laneNumber, payableToll, operatorName);

            TollTransaction cashTxn = new TollTransaction(txnId, vehicleRegNo, plazaId, laneNumber, null, crossingTime,
                    payableToll, vehicleType, vehicleType, TripType.SINGLE, PaymentType.CASH, TransactionStatus.SUCCESS,
                    false, operatorName, null);
            transactionRepository.save(cashTxn);

            String penaltyNote = isDoubleTollPenalty ? " [DOUBLE TOLL PENALTY (2x) APPLIED]" : "";
            return new CrossingResponse(true, cashTxn,
                    String.format("Cash toll Rs. %.2f collected by Operator '%s'.%s Barrier OPENED.",
                            payableToll, operatorName, penaltyNote),
                    false, isDoubleTollPenalty);
        } finally {
            lane.setProcessingVehicle(false);
        }
    }

    @Override
    public CrossingResponse processEmergencyCrossing(int plazaId, int laneNumber, String vehicleRegNo,
                                                    String emergencyType, LocalDateTime crossingTime) {
        String txnId = UUID.randomUUID().toString();
        TollPlaza plaza = plazaService.getPlaza(plazaId)
                .orElseThrow(() -> new IllegalArgumentException("Plaza #" + plazaId + " not found."));

        Lane lane = plaza.findLane(laneNumber)
                .orElseThrow(() -> new IllegalArgumentException("Lane #" + laneNumber + " not found in Plaza #" + plazaId));

        lane.setProcessingVehicle(true);
        try {
            // Rule: "Emergency vehicles (ambulance, fire engine) use the Emergency lane and pay nothing."
            TollTransaction emgTxn = new TollTransaction(txnId, vehicleRegNo, plazaId, laneNumber, null, crossingTime,
                    0.0, VehicleType.VAN, VehicleType.VAN, TripType.EMERGENCY, PaymentType.EXEMPT, TransactionStatus.SUCCESS,
                    false, "EMERGENCY_OVERRIDE", null);
            transactionRepository.save(emgTxn);

            return new CrossingResponse(true, emgTxn,
                    String.format("Emergency Vehicle (%s: %s) cleared via Emergency Lane #%d. Toll = Rs. 0.00. Barrier OPENED.",
                            emergencyType, vehicleRegNo, laneNumber),
                    false, false);
        } finally {
            lane.setProcessingVehicle(false);
        }
    }
}
