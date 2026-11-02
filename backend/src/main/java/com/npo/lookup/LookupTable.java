package com.npo.lookup;

import java.util.HashMap;
import java.util.Map;

import org.bson.Document;

import com.npo.common.DatabaseConnection;

public abstract class LookupTable {

    protected String id;
    protected String name;

    protected LookupTable() {
    }

    protected LookupTable(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    // Subclass hooks
    protected abstract String getCollectionName();

    protected abstract void addExtraFields(Map<String, Object> data);

    protected abstract void loadExtraFields(Document doc);

    // CRUD
    public void create() {
        validate();

        id = DatabaseConnection.getInstance()
                .insert(getCollectionName(), toMap());
    }

    public LookupTable read(String id) {
        Document doc = DatabaseConnection.getInstance()
                .selectById(getCollectionName(), id);

        if (doc == null) {
            return null;
        }

        fromDocument(doc);
        return this;
    }

    public void edit() {
        requireId();
        validate();

        DatabaseConnection.getInstance()
                .update(getCollectionName(), id, toMap());
    }

    public void delete() {
        requireId();

        DatabaseConnection.getInstance()
                .delete(getCollectionName(), id);

        id = null;
    }

    // Mapping to/from MongoDB
    public Map<String, Object> toMap() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        addExtraFields(data);
        return data;
    }

    public void fromDocument(Document doc) {
        this.id = doc.getObjectId("_id").toHexString();
        this.name = doc.getString("name");
        loadExtraFields(doc);
    }

    public void populate(Map<String, Object> body) {
        if (body == null) {
            throw new IllegalArgumentException("Request body is required");
        }

        Object value = body.get("name");

        if (value != null && !(value instanceof String)) {
            throw new IllegalArgumentException("Name must be text");
        }

        this.name = (String) value;
        loadExtraFields(new Document(body));
    }

    // Shared validation before saving
    protected void validate() {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }

        name = name.strip();

        if (name.length() > 120) {
            throw new IllegalArgumentException(
                    "Name must not exceed 120 characters"
            );
        }
    }

    private void requireId() {
        if (id == null) {
            throw new IllegalStateException(
                    "Record has no id: call create() or read(id) first"
            );
        }
    }
}
