package com.restaurant.dto.payment;

import java.math.BigDecimal;

/**
 * Cashier attestation and cash tender are rechecked against the locked order.
 */
public record PaymentConfirmation(String paymentMethod, boolean paymentVerified,
    BigDecimal amountReceived) {
}
