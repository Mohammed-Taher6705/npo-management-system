package com.npo.lookup;

import java.util.Map;

import org.bson.Document;

public class ItemCategory extends LookupTable {

    private String unit;

    public ItemCategory() {
    }

    public ItemCategory(String name, String unit) {
        super(name);
        this.unit = unit;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    @Override
    protected String getCollectionName() {
        return "item_categories";
    }

    @Override
    protected void addExtraFields(Map<String, Object> data) {
        data.put("unit", unit);
    }

    @Override
    protected void loadExtraFields(Document doc) {
        Object value = doc.get("unit");

        if (value != null && !(value instanceof String)) {
            throw new IllegalArgumentException("Unit must be text");
        }

        unit = (String) value;
    }

    @Override
    protected void validate() {
        super.validate();

        if (unit == null || unit.isBlank()) {
            throw new IllegalArgumentException("Unit is required");
        }

        unit = unit.strip();

        if (unit.length() > 500) {
            throw new IllegalArgumentException(
                    "Unit must not exceed 500 characters"
            );
        }
    }
}