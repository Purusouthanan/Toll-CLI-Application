package com.toll.module5.billing.payment;

import com.toll.common.PaymentType;
import com.toll.module2.account.service.DebitResult;
import com.toll.module2.account.service.IAccountService;

/**
 * Polymorphic payment handler for Electronic FASTag deductions.
 * Interacts directly with Module 2 AccountService.
 */
public class FASTagPaymentMethod implements PaymentMethod {
    private final IAccountService accountService;

    public FASTagPaymentMethod(IAccountService accountService) {
        if (accountService == null) throw new IllegalArgumentException("Account service cannot be null.");
        this.accountService = accountService;
    }

    @Override
    public PaymentType getPaymentType() {
        return PaymentType.FASTAG;
    }

    @Override
    public PaymentResult processPayment(PaymentContext context) {
        if (context.tagId() == null || context.tagId().trim().isEmpty()) {
            return PaymentResult.failure(PaymentType.FASTAG, context.amount(), "No FASTag ID provided.");
        }

        DebitResult debitResult = accountService.deductToll(context.tagId(), context.amount());
        if (debitResult.isSuccessful()) {
            return PaymentResult.fastagSuccess(
                    context.amount(),
                    context.tagId(),
                    debitResult.getRemainingBalance(),
                    debitResult.isAutoBlocked(),
                    debitResult.isLowBalanceAlertTriggered()
            );
        } else {
            return PaymentResult.failure(PaymentType.FASTAG, context.amount(), debitResult.getMessage());
        }
    }
}
