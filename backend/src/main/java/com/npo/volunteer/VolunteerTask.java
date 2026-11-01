package com.npo.volunteer;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bson.Document;

import com.npo.common.DatabaseConnection;
import com.npo.common.IObserver;
import com.npo.common.ISubject;
import com.npo.user.Volunteer;

/**
 * Observer pattern: the Subject.
 * Assigned volunteers are observers; they are notified whenever the task changes.
 */
public class VolunteerTask implements ISubject {
    private String id;
    private String title;
    private Date date;
    private String location;
    private List<Volunteer> assignees = new ArrayList<>();
    private List<IObserver> observers = new ArrayList<>();   // not saved; rebuilt from assignees
    private String lastChange = "";                           // text sent to observers

    public VolunteerTask() {
    }

    public VolunteerTask(String title, Date date, String location) {
        this.title = title;
        this.date = date;
        this.location = location;
    }

    // ----- CRUD -----
    public void create() {
        id = DatabaseConnection.getInstance().insert("volunteerTasks", toDocument());
    }

    public VolunteerTask read(String id) {
        Document doc = DatabaseConnection.getInstance().selectById("volunteerTasks", id);
        if (doc == null) {
            return null;
        }
        this.id = id;
        this.title = doc.getString("title");
        this.date = doc.getDate("date");
        this.location = doc.getString("location");

        assignees = new ArrayList<>();
        observers = new ArrayList<>();
        List<String> volunteerIds = doc.getList("volunteerIds", String.class);
        for (String volunteerId : volunteerIds) {
            Volunteer v = new Volunteer();
            v.read(volunteerId);
            assignees.add(v);
            attach(v);                    // every assignee observes the task
        }
        return this;
    }

    public void edit() {
        DatabaseConnection.getInstance().update("volunteerTasks", id, toDocument());
    }

    public void delete() {
        lastChange = "Task '" + title + "' was cancelled";
        notifyObservers();
        DatabaseConnection.getInstance().delete("volunteerTasks", id);
    }

    // ----- assignments -----
    public boolean assignVolunteer(Volunteer v) {
        if (!v.isAvailableOn(date)) {
            return false;
        }
        assignees.add(v);
        attach(v);
        edit();
        lastChange = "You were assigned to task '" + title + "' on " + date + " at " + location;
        v.update(this);
        return true;
    }

    public void removeVolunteer(Volunteer v) {
        for (int i = 0; i < assignees.size(); i++) {
            if (assignees.get(i).getId().equals(v.getId())) {
                detach(assignees.get(i));
                assignees.remove(i);
                break;
            }
        }
        edit();
    }

    public List<Volunteer> getAssignees() {
        return assignees;
    }

    // ----- changes that notify all assignees -----
    public void changeDate(Date newDate) {
        date = newDate;
        edit();
        lastChange = "Task '" + title + "' moved to " + date;
        notifyObservers();
    }

    public void changeLocation(String newLocation) {
        location = newLocation;
        edit();
        lastChange = "Task '" + title + "' moved to " + location;
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
        return lastChange;
    }

    // ----- getters -----
    public String getId() { return id; }
    public String getTitle() { return title; }
    public Date getDate() { return date; }
    public String getLocation() { return location; }

    // fields saved to MongoDB (volunteers as a list of ids)
    private Document toDocument() {
        List<String> volunteerIds = new ArrayList<>();
        for (Volunteer v : assignees) {
            volunteerIds.add(v.getId());
        }
        return new Document("title", title)
                .append("date", date)
                .append("location", location)
                .append("volunteerIds", volunteerIds);
    }
}
