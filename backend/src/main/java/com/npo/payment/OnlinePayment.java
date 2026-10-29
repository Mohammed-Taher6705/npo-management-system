package com.npo.payment;

import java.util.UUID;

/**
 * Concrete Strategy: online card payment.
 * Phase 1 simulates the gateway: it accepts the payment and generates a transaction id.
 * Never store real card numbers; a real gateway (Phase 2) would return the transaction id.
 */
public class OnlinePayment implements IPaymentMethod {

    private String cardHolder;
    private String transactionId;

    public OnlinePayment(String cardHolder) {
        this.cardHolder = cardHolder;
    }

    @Override
    public boolean pay(double amount) {
        if (!validate() || amount <= 0) {
            return false;
        }
        transactionId = "TX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return true;
    }

    @Override
    public boolean refund(double amount) {
        return validate() && amount > 0 && transactionId != null;   // only paid transactions can be refunded
    }

    @Override
    public boolean validate() {
        return cardHolder != null && !cardHolder.trim().isEmpty();
    }

    public String getCardHolder() { return cardHolder; }
    public String getTransactionId() { return transactionId; }
}
