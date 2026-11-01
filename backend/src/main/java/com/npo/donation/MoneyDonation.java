package com.npo.donation;

import org.bson.Document;

import com.npo.common.DatabaseConnection;
import com.npo.payment.IPaymentMethod;
import com.npo.user.Donor;

public class MoneyDonation extends Donation {
    private double amount;
    private String currency;
    private IPaymentMethod paymentMethod;     // Strategy pattern: the context holds the strategy

    public MoneyDonation() {
        type = "MONEY";
    }

    public MoneyDonation(Donor donor, double amount, String currency) {
        super(donor, "MONEY");
        this.amount = amount;
        this.currency = currency;
    }

    @Override
    public double getAmount() {
        return amount;
    }

    @Override
    public void create() {
        super.create();
        Document data = new Document("amount", amount)
                .append("currency", currency);
        DatabaseConnection.getInstance().update("donations", id, data);
    }

    @Override
    public MoneyDonation read(String id) {
        if (super.read(id) == null) {
            return null;
        }
        Document doc = DatabaseConnection.getInstance().selectById("donations", id);
        amount = doc.getDouble("amount");
        currency = doc.getString("currency");
        return this;
    }

    public void setPaymentMethod(IPaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    // uses the chosen payment strategy, then saves the result
    public boolean processPayment() {
        if (paymentMethod.pay(amount)) {
            status = "COMPLETED";
            donor.addToTotal(amount);          // count it only when the money is actually paid
            announceReceived();
        } else {
            status = "FAILED";
        }
        edit();
        return status.equals("COMPLETED");
    }

    public String getCurrency() { return currency; }
}
