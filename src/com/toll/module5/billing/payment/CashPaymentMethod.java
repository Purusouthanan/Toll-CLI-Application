package com.toll.module5.billing.payment;

import com.toll.common.PaymentType;
import java.util.UUID;

/**
 * Polymorphic payment handler for manual Cash lane payments.
 * Records collecting lane operator per problem requirements.
 */
public class CashPaymentMethod implements PaymentMethod {

    @Override
    public PaymentType getPaymentType() {
        return PaymentType.CASH;
    }

    @Override
    public PaymentResult processPayment(PaymentContext context) {
        String operator = (context.operatorName() != null && !context.operatorName().trim().isEmpty())
                ? context.operatorName().trim()
                : "Station Operator #1";

        String receiptId = "RCPT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return PaymentResult.cashSuccess(context.amount(), operator, receiptId);
    }
}
