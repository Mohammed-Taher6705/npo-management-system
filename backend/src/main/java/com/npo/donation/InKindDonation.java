package com.npo.donation;

import org.bson.Document;

import com.npo.common.DatabaseConnection;
import com.npo.user.Donor;

public class InKindDonation extends Donation {
    private String itemName;
    private int quantity;
    private double estimatedValue;

    public InKindDonation() {
        type = "IN_KIND";
    }

    public InKindDonation(Donor donor, String itemName, int quantity, double estimatedValue) {
        super(donor, "IN_KIND");
        this.itemName = itemName;
        this.quantity = quantity;
        this.estimatedValue = estimatedValue;
    }

    @Override
    public double getAmount() {
        return estimatedValue;
    }

   @Override
public void create() {
    super.create();
    Document data = new Document("itemName", itemName)
            .append("quantity", quantity)
            .append("estimatedValue", estimatedValue);
    DatabaseConnection.getInstance().update("donations", id, data);

    donor.addToTotal(estimatedValue);     // items count as soon as they are received
    announceReceived();
}

    @Override
    public InKindDonation read(String id) {
        if (super.read(id) == null) {
            return null;
        }
        Document doc = DatabaseConnection.getInstance().selectById("donations", id);
        itemName = doc.getString("itemName");
        quantity = doc.getInteger("quantity");
        estimatedValue = doc.getDouble("estimatedValue");
        return this;
    }

    public String getItemName() { return itemName; }
    public int getQuantity() { return quantity; }
}
