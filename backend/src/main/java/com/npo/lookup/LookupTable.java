package com.npo.lookup;

import java.util.HashMap;
import java.util.Map;

import org.bson.Document;

import com.npo.common.DatabaseConnection;


public abstract class LookupTable {

    protected String id;      // MongoDB _id as hex string 
    protected String name;

    protected LookupTable() { }

    protected LookupTable(String name) {
        this.name = name;
    }

    // ----- getters / setters -----
    public String getId() { 
      return id; 
    }
    public String getName() { 
      return name; 
    }
    public void setName(String name) {
       this.name = name; 
    }

    // ----- subclass hooks -----
    protected abstract String getCollectionName();
    protected abstract void addExtraFields(Map<String, Object> data);
    protected abstract void loadExtraFields(Document doc);

    // ----- CRUD -----
    public void create() {
        id = DatabaseConnection.getInstance().insert(getCollectionName(), toMap());
    }

    /** Loads the record with this id into this object. Returns null if not found. */
    public LookupTable read(String id) {
        Document doc = DatabaseConnection.getInstance().selectById(getCollectionName(), id);
        if (doc == null) return null;
        fromDocument(doc);
        return this;
    }

    public void edit() {
        requireId();
        DatabaseConnection.getInstance().update(getCollectionName(), id, toMap());
    }

    public void delete() {
        requireId();
        DatabaseConnection.getInstance().delete(getCollectionName(), id);
        id = null;
    }

    // ----- mapping to/from MongoDB -----
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
        Object n = body.get("name");
        this.name = (n == null) ? null : n.toString();
        loadExtraFields(new Document(body));
    }

    private void requireId() {
        if (id == null) {
            throw new IllegalStateException("Record has no id: call create() or read(id) first");
        }
    }
}
