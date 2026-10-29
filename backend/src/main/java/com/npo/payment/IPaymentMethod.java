package com.npo.payment;

/**
 * Strategy pattern: the Strategy interface.
 * Every payment method is one interchangeable algorithm for paying/refunding.
 * The context (MoneyDonation) only knows this interface, never the concrete class.
 */
public interface IPaymentMethod {

    /** Takes the payment. Returns true if it was accepted. */
    boolean pay(double amount);

    /** Returns money to the donor. Returns true if it was accepted. */
    boolean refund(double amount);

    /** Checks that this method has all the details it needs. */
    boolean validate();
}
