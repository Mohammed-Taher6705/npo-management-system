package com.npo.beneficiary;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.client.model.Filters;
import com.npo.common.DatabaseConnection;
import com.npo.common.IObserver;
import com.npo.common.ISubject;
import com.npo.lookup.NeedCategory;
import com.npo.user.Admin;
import com.npo.user.Beneficiary;

public class Need implements ISubject {
    private String id;
    private NeedCategory category;     // e.g. Food, Medical, Shelter (lookup table)
    private String priority;           // "LOW", "MEDIUM", "HIGH"
    private String status;             // "OPEN", "FULFILLED"
    private List<IObserver> observers = new ArrayList<>();
    private String alertMessage = "";

    public Need() {
    }

    public Need(NeedCategory category, String priority) {
        this.category = category;
        this.priority = priority;
        this.status = "OPEN";
    }

    public void create() {
        Document data = new Document("categoryId", category.getId())
                .append("priority", priority)
                .append("status", status);

        id = DatabaseConnection.getInstance().insert("needs", data);
    }

    public Need read(String id) {
        Document doc = DatabaseConnection.getInstance().selectById("needs", id);
        if (doc == null) {
            return null;
        }

        this.id = id;
        this.priority = doc.getString("priority");
        this.status = doc.getString("status");

        this.category = new NeedCategory();
        this.category.read(doc.getString("categoryId"));
        return this;
    }

    public void edit() {
        Document data = new Document("priority", priority)
                .append("status", status);

        DatabaseConnection.getInstance().update("needs", id, data);
    }

    public void delete() {
        DatabaseConnection.getInstance().delete("needs", id);
    }

    // ----- client requirement: urgent-need alert -----
    public boolean isUrgent() {
        return "HIGH".equals(priority);
    }

    public void announceUrgent(Beneficiary b) {
        alertMessage = "URGENT: " + b.getName() + " needs " + category.getName() + " (HIGH priority)";
        observers = new ArrayList<>();
        for (Document doc : DatabaseConnection.getInstance().select("users", Filters.eq("role", "ADMIN"))) {
            Admin admin = new Admin();
            admin.read(doc.getObjectId("_id").toHexString());
            attach(admin);
        }
        notifyObservers();
    }

    // ----- Observer pattern (ISubject) -----
    @Override
    public void attach(IObserver o) {
        observers.add(o);
    }

    @Override
    public void detach(IObserver o) {
        observers.remove(o);
    }

    @Override
    public void notifyObservers() {
        for (IObserver o : observers) {
            o.update(this);
        }
    }

    @Override
    public String getUpdateMessage() {
        return alertMessage;
    }

    // ----- getters / setters (from the diagram) -----
    public String getId() { return id; }
    public NeedCategory getCategory() { return category; }
    public String getPriority() { return priority; }
    public String getStatus() { return status; }

    public void setPriority(String priority) { this.priority = priority; }
    public void setStatus(String status) { this.status = status; }
}