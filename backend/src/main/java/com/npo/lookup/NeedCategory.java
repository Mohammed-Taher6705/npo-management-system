package com.npo.lookup;

import java.util.Map;

import org.bson.Document;

public class NeedCategory extends LookupTable {

    private String description;

    public NeedCategory() {
    }

    public NeedCategory(String name, String description) {
        super(name);
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    protected String getCollectionName() {
        return "need_categories";
    }

    @Override
    protected void addExtraFields(Map<String, Object> data) {
        data.put("description", description);
    }

    @Override
    protected void loadExtraFields(Document doc) {
        Object value = doc.get("description");

        if (value != null && !(value instanceof String)) {
            throw new IllegalArgumentException(
                    "Description must be text"
            );
        }

        description = (String) value;
    }

    @Override
    protected void validate() {
        super.validate();

        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException(
                    "Description is required"
            );
        }

        description = description.strip();

        if (description.length() > 500) {
            throw new IllegalArgumentException(
                    "Description must not exceed 500 characters"
            );
        }
    }
}