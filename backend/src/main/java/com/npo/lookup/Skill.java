package com.npo.lookup;

import java.util.Map;

import org.bson.Document;

public class Skill extends LookupTable {

    private String level;

    public Skill() {
    }

    public Skill(String name, String level) {
        super(name);
        this.level = level;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    @Override
    protected String getCollectionName() {
        return "skills";
    }

    @Override
    protected void addExtraFields(Map<String, Object> data) {
        data.put("level", level);
    }

    @Override
    protected void loadExtraFields(Document doc) {
        Object value = doc.get("level");

        if (value != null && !(value instanceof String)) {
            throw new IllegalArgumentException("Level must be text");
        }

        level = (String) value;
    }

    @Override
    protected void validate() {
        super.validate();

        if (level == null || level.isBlank()) {
            throw new IllegalArgumentException("Level is required");
        }

        level = level.strip();

        if (!level.equals("Beginner")
                && !level.equals("Intermediate")
                && !level.equals("Advanced")) {
            throw new IllegalArgumentException(
                    "Level must be Beginner, Intermediate or Advanced"
            );
        }
    }
}