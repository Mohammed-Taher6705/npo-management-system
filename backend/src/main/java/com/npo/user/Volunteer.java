package com.npo.user;

import org.bson.Document;
import com.npo.common.DatabaseConnection;
import com.npo.lookup.Skill;

import java.util.ArrayList;
import java.util.List;

import java.util.Date;


public class Volunteer extends User {
    private List<Skill> skills = new ArrayList<>();
    private String availability;
    private double totalHours;

    public Volunteer(String name, String email, String phone, String password,
                     String preferredChannel, String availability) {
        super(name, email, phone, password, preferredChannel, "VOLUNTEER");
        this.availability = availability;
        this.totalHours = 0;
    }

    public Volunteer() {
       role = "VOLUNTEER";
    }

    public void addSkill(Skill s) {
        skills.add(s);
        edit();
    }

    public void removeSkill(Skill s) {
        for (int i = 0; i < skills.size(); i++) {
            if (skills.get(i).getId().equals(s.getId())) {
                skills.remove(i);
                break;
            }
        }
        edit();
    }

    public void setAvailability(String a) {
        this.availability = a;
    }

    public double getHours() {
        return totalHours;
    }

    public void addHours(double h) {
       totalHours = totalHours + h;
        edit();
    }

    public List<Skill> getSkills() { 
        return skills; 
    }

    public String getAvailability() { 
        return availability; 
    }

    private void saveVolunteerFields() {
        List<String> skillIds = new ArrayList<>();
        for (Skill s : skills) {
            skillIds.add(s.getId());
        }
        Document data = new Document("availability", availability)
                .append("totalHours", totalHours)
                .append("skillIds", skillIds)
                .append("unavailableDates", unavailableDates);
        DatabaseConnection.getInstance().update("users", id, data);
    }

    @Override
    public void create() {
        super.create();
        saveVolunteerFields();
    }

    @Override
    public void edit() {
        super.edit();
        saveVolunteerFields();
    }

    @Override
    public Volunteer read(String id) {
        if (super.read(id) == null) {
            return null;
        }
        Document doc = DatabaseConnection.getInstance().selectById("users", id);
        availability = doc.getString("availability");
        totalHours = doc.getDouble("totalHours");

        unavailableDates = new ArrayList<>();
        List<Date> saved = doc.getList("unavailableDates", Date.class);
        if (saved != null) {
            unavailableDates.addAll(saved);
        }

        skills = new ArrayList<>();
        List<String> skillIds = doc.getList("skillIds", String.class);
        for (String skillId : skillIds) {
            Skill s = new Skill();
            s.read(skillId);
            skills.add(s);
        }
        return this;
    }

    @Override
    public boolean login(String email, String password) {
        if (!super.login(email, password)) {
            return false;
        }
        read(id);
        return true;
    }

    private List<Date> unavailableDates = new ArrayList<>();

    public void addUnavailableDate(Date d) {
        if (isAvailableOn(d)) {
            unavailableDates.add(d);
            edit();
        }
    }

    public void removeUnavailableDate(Date d) {
    for (int i = 0; i < unavailableDates.size(); i++) {
        if (unavailableDates.get(i).equals(d)) {
            unavailableDates.remove(i);
            break;
        }
    }
        edit();
    }

    public boolean isAvailableOn(Date d) {
    for (Date u : unavailableDates) {
        if (u.equals(d)) {
            return false;
        }
    }
     return true;
    }

    public List<Date> getUnavailableDates() {
        return unavailableDates;
    }





}
