package com.toll.module5.billing.payment;

import com.toll.common.PaymentType;

/**
 * Strategy interface demonstrating Abstraction and Polymorphism.
 * Used by the Toll Processing Service to polymorphically execute either
 * FASTag or Cash payments without knowing internal payment channel details.
 */
public interface PaymentMethod {
    PaymentType getPaymentType();
    PaymentResult processPayment(PaymentContext context);
}
