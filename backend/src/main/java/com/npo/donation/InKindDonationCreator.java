package com.npo.donation;

import com.npo.user.Donor;

public class InKindDonationCreator extends DonationCreator {
    private String itemName;
    private int quantity;
    private double estimatedValue;

    public InKindDonationCreator(String itemName, int quantity, double estimatedValue) {
        this.itemName = itemName;
        this.quantity = quantity;
        this.estimatedValue = estimatedValue;
    }

    @Override
    public Donation createDonation(Donor donor) {
        return new InKindDonation(donor, itemName, quantity, estimatedValue);
    }
}
