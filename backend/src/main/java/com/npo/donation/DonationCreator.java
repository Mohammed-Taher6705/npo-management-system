package com.npo.donation;

import com.npo.user.Donor;

/**
 * Factory Method pattern: the Creator.
 * Subclasses decide WHICH donation object is created.
 */
public abstract class DonationCreator {

    // the factory method: each subclass creates its own donation type
    public abstract Donation createDonation(Donor donor);

    // shared steps for every donation, whatever its type
    public Donation registerDonation(Donor donor) {
        Donation donation = createDonation(donor);     // subclass decides the type
        donation.create();                             // save to MongoDB
        return donation;
    }
}
