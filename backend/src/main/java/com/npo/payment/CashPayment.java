package com.npo.payment;


/** Concrete Strategy: the donor hands cash to a staff member. */
public class CashPayment implements IPaymentMethod {

    private String receivedBy;      // staff member who took the cash

    public CashPayment(String receivedBy) {
        this.receivedBy = receivedBy;
    }

    @Override
    public boolean pay(double amount) {
        return validate() && amount > 0;
    }

    @Override
    public boolean refund(double amount) {
        return validate() && amount > 0;
    }

    @Override
    public boolean validate() {
        return receivedBy != null && !receivedBy.trim().isEmpty();
    }

    public String getReceivedBy() { return receivedBy; }
}
