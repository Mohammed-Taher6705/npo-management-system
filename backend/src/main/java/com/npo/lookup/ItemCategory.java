package com.npo.lookup;

import java.util.Map;

import org.bson.Document;

public class ItemCategory extends LookupTable {
    private String unit;

    public ItemCategory() { }

    public ItemCategory(String name, String unit) {
        super(name);
        this.unit = unit;
    }

    // ----- getters / setters -----
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
      this.unit = doc.getString("unit"); 
    }

}
