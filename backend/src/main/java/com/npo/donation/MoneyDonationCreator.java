package com.npo.donation;

import com.npo.user.Donor;

public class MoneyDonationCreator extends DonationCreator {
    private double amount;
    private String currency;

    public MoneyDonationCreator(double amount, String currency) {
        this.amount = amount;
        this.currency = currency;
    }

    @Override
    public Donation createDonation(Donor donor) {
            MoneyDonation d = new MoneyDonation(donor, amount, currency);
    d.setPaymentMethod(paymentMethod);
    return d;
    }
}
