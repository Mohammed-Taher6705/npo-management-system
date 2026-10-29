package com.npo.lookup;

import java.util.Map;

import org.bson.Document;

public class Skill extends LookupTable {
    private String level;

    public Skill() { }
  
    public Skill(String name, String level) {
        super(name);
        this.level = level;
    }

    // ----- getters / setters -----
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
       this.level = doc.getString("level"); 
    }
}
