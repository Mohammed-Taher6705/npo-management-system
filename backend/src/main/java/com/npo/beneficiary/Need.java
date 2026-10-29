package com.npo.beneficiary;

import org.bson.Document;

import com.npo.common.DatabaseConnection;
import com.npo.lookup.NeedCategory;

public class Need {
    private String id;
    private NeedCategory category;     // e.g. Food, Medical, Shelter (lookup table)
    private String priority;           // "LOW", "MEDIUM", "HIGH"
    private String status;             // "OPEN", "FULFILLED"

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

    // ----- getters / setters (from the diagram) -----
    public String getId() { return id; }
    public NeedCategory getCategory() { return category; }
    public String getPriority() { return priority; }
    public String getStatus() { return status; }

    public void setPriority(String priority) { this.priority = priority; }
    public void setStatus(String status) { this.status = status; }
}
