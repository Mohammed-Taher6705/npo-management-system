package com.npo.user;
import java.util.ArrayList;
import java.util.List;
import com.mongodb.client.model.Filters;
import com.npo.donation.Donation;
import com.npo.donation.InKindDonation;
import com.npo.donation.MoneyDonation;
import org.bson.Document;

import com.npo.common.DatabaseConnection;

public class Donor extends User {
    private String donorType;        // "INDIVIDUAL" or "ORGANIZATION"
    private double totalDonated;

    public Donor() {
        role = "DONOR";
    }

    public Donor(String name, String email, String phone, String password,
                 String preferredChannel, String donorType) {
        super(name, email, phone, password, preferredChannel, "DONOR");
        this.donorType = donorType;
        this.totalDonated = 0;
    }

    @Override
    public boolean login(String email, String password) {
        if (!super.login(email, password)) {
            return false;
        }
        read(id);
        return true;
    }

    // save the shared User fields, then add the Donor fields
    @Override
    public void create() {
        super.create();
        Document data = new Document("donorType", donorType)
                .append("totalDonated", totalDonated);
        DatabaseConnection.getInstance().update("users", id, data);
    }

    // load the shared User fields, then the Donor fields
    @Override
    public Donor read(String id) {
        if (super.read(id) == null) {
            return null;
        }
        Document doc = DatabaseConnection.getInstance().selectById("users", id);
        donorType = doc.getString("donorType");
        totalDonated = doc.getDouble("totalDonated");
        return this;
    }

    @Override
    public void edit() {
        super.edit();
        Document data = new Document("donorType", donorType)
                .append("totalDonated", totalDonated);
        DatabaseConnection.getInstance().update("users", id, data);
    }

    // called after each successful donation
    public void addToTotal(double amount) {
        totalDonated = totalDonated + amount;
        edit();
    }

    public String getDonorType() { return donorType; }
    public double getTotalDonated() { return totalDonated; }
    public void setDonorType(String donorType) { this.donorType = donorType; }

// all donations made by this donor, loaded from the "donations" collection
public List<Donation> getDonations() {
    List<Donation> result = new ArrayList<>();
    for (Document doc : DatabaseConnection.getInstance().select("donations", Filters.eq("donorId", id))) {
        Donation d;
        if (doc.getString("type").equals("MONEY")) {
            d = new MoneyDonation();
        } else {
            d = new InKindDonation();
        }
        d.read(doc.getObjectId("_id").toHexString());
        result.add(d);
    }
    return result;
}
}
