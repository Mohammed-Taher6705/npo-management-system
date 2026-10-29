package com.npo.donation;
import java.util.Date;
import org.bson.Document;
import com.npo.common.DatabaseConnection;
import com.npo.user.Donor;

/**
 * Factory Method pattern: the Product.
 * A donation is always MoneyDonation or InKindDonation, so this class is abstract.
 * All donations are stored in one MongoDB collection ("donations"); the "type" field tells them apart.
 */
public abstract class Donation {
    protected String id;
    protected Donor donor;
    protected Date date;
    protected String status;
    protected String type;          // "MONEY" or "IN_KIND"

    public Donation() {
    }

    public Donation(Donor donor, String type) {
        this.donor = donor;
        this.type = type;
        this.date = new Date();     // today
        this.status = "PENDING";
    }

    // each subclass calculates its own value
    public abstract double getAmount();

    public void create() {
        Document data = new Document("type", type)
                .append("donorId", donor.getId())
                .append("date", date)
                .append("status", status);

        id = DatabaseConnection.getInstance().insert("donations", data);
    }

    public Donation read(String id) {
        Document doc = DatabaseConnection.getInstance().selectById("donations", id);
        if (doc == null) {
            return null;
        }

        this.id = id;
        this.type = doc.getString("type");
        this.date = doc.getDate("date");
        this.status = doc.getString("status");

        this.donor = new Donor();
        this.donor.read(doc.getString("donorId"));
        return this;
    }

    public void edit() {
        Document data = new Document("status", status);
        DatabaseConnection.getInstance().update("donations", id, data);
    }

    public void delete() {
        DatabaseConnection.getInstance().delete("donations", id);
    }

    public String getId() { return id; }
    public Donor getDonor() { return donor; }
    public Date getDate() { return date; }
    public String getStatus() { return status; }
    public String getType() { return type; }

    public void setStatus(String status) { this.status = status; }
}
