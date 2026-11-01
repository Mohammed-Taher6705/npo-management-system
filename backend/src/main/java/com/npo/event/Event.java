package com.npo.event;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bson.Document;

import com.npo.common.DatabaseConnection;
import com.npo.common.IObserver;
import com.npo.common.ISubject;
import com.npo.ticket.ITicket;
import com.npo.user.User;

/**
 * Observer pattern: the Subject.
 * Registered attendees are observers; they are notified when the event changes.
 */
public class Event implements ISubject {
    private String id;
    private String title;
    private String eventType;   // "FUNDRAISER", "WORKSHOP"
    private Date date;
    private String location;
    private int capacity;
    private String status;      // "UPCOMING", "ONGOING", "COMPLETED", "CANCELLED"
    private List<Registration> registrations = new ArrayList<>();
    private List<Registration> waitingList = new ArrayList<>();   // client requirement
    private List<IObserver> observers = new ArrayList<>();
    private String lastChange = "";                               // text sent to observers

    public Event() {
    }

    public Event(String title, String eventType, Date date, String location, int capacity) {
        this.title = title;
        this.eventType = eventType;
        this.date = date;
        this.location = location;
        this.capacity = capacity;
        this.status = "UPCOMING";
    }

    public void create() {
        id = DatabaseConnection.getInstance().insert("events", toDocument());
    }

    public Event read(String id) {
        Document doc = DatabaseConnection.getInstance().selectById("events", id);
        if (doc == null) {
            return null;
        }
        this.id = id;
        this.title = doc.getString("title");
        this.eventType = doc.getString("eventType");
        this.date = doc.getDate("date");
        this.location = doc.getString("location");
        this.capacity = doc.getInteger("capacity");
        this.status = doc.getString("status");

        waitingList = new ArrayList<>();
        List<Document> waitDocs = doc.getList("waitingList", Document.class);
        if (waitDocs != null) {                       // old events have no waitingList
            for (Document w : waitDocs) {
                Registration r = new Registration();
                r.load(w);
                waitingList.add(r);
            }
        }

        registrations = new ArrayList<>();
        observers = new ArrayList<>();
        List<Document> regDocs = doc.getList("registrations", Document.class);
        for (Document regDoc : regDocs) {
            Registration r = new Registration();
            r.load(regDoc);
            registrations.add(r);
            attach(r.getAttendee());          // every attendee observes the event
        }
        return this;
    }

    public void edit() {
        DatabaseConnection.getInstance().update("events", id, toDocument());
    }

    public void delete() {
        changeStatus("CANCELLED");            // attendees are told before it disappears
        DatabaseConnection.getInstance().delete("events", id);
    }

    // returns false if the event is full, not upcoming, or the user is already registered
    public boolean register(User u, ITicket t) {
        if (!status.equals("UPCOMING") || isFull() || isRegistered(u)) {
            return false;
        }
        registrations.add(new Registration(u, t));
        attach(u);
        edit();
        lastChange = "You are registered for '" + title + "' on " + date
                + " (" + t.getDescription() + ", " + t.getPrice() + ")";
        u.update(this);                       // tell only the new attendee
        return true;
    }

    public void cancelRegistration(User u) {
        for (int i = 0; i < registrations.size(); i++) {
            if (registrations.get(i).getAttendee().getId().equals(u.getId())) {
                detach(registrations.get(i).getAttendee());
                registrations.remove(i);
                break;
            }
        }
        edit();
        promoteFromWaitingList();             // free spot goes to the first one waiting
    }

    public List<User> getAttendees() {
        List<User> attendees = new ArrayList<>();
        for (Registration r : registrations) {
            attendees.add(r.getAttendee());
        }
        return attendees;
    }

    public void changeStatus(String newStatus) {
        status = newStatus;
        edit();
        lastChange = "Event '" + title + "' is now " + status;
        notifyObservers();
    }

    // ----- client requirement: waiting list -----
    public boolean isFull() {
        return registrations.size() >= capacity;
    }

    public boolean joinWaitingList(User u, ITicket t) {
        if (!status.equals("UPCOMING") || !isFull() || isRegistered(u) || isWaiting(u)) {
            return false;
        }
        waitingList.add(new Registration(u, t));
        edit();
        lastChange = "'" + title + "' is full. You are number " + waitingList.size() + " on the waiting list.";
        u.update(this);
        return true;
    }

    public void leaveWaitingList(User u) {
        for (int i = 0; i < waitingList.size(); i++) {
            if (waitingList.get(i).getAttendee().getId().equals(u.getId())) {
                waitingList.remove(i);
                break;
            }
        }
        edit();
    }

    public List<User> getWaitingList() {
        List<User> users = new ArrayList<>();
        for (Registration r : waitingList) {
            users.add(r.getAttendee());
        }
        return users;
    }

    private void promoteFromWaitingList() {
        if (!status.equals("UPCOMING") || isFull() || waitingList.isEmpty()) {
            return;
        }
        Registration first = waitingList.remove(0);
        registrations.add(first);
        attach(first.getAttendee());
        edit();
        lastChange = "A spot opened: you are now registered for '" + title + "' on " + date;
        first.getAttendee().update(this);
    }

    private boolean isRegistered(User u) {
        for (Registration r : registrations) {
            if (r.getAttendee().getId().equals(u.getId())) {
                return true;
            }
        }
        return false;
    }

    private boolean isWaiting(User u) {
        for (Registration r : waitingList) {
            if (r.getAttendee().getId().equals(u.getId())) {
                return true;
            }
        }
        return false;
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

    // ----- getters / setters -----
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getEventType() { return eventType; }
    public Date getDate() { return date; }
    public String getLocation() { return location; }
    public int getCapacity() { return capacity; }
    public String getStatus() { return status; }
    public List<Registration> getRegistrations() { return registrations; }

    public void setTitle(String title) { this.title = title; }
    public void setDate(Date date) { this.date = date; }
    public void setLocation(String location) { this.location = location; }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
        while (!isFull() && !waitingList.isEmpty() && status.equals("UPCOMING")) {
            promoteFromWaitingList();         // more seats -> people waiting get them
        }
    }

    private Document toDocument() {
        List<Document> regDocs = new ArrayList<>();
        for (Registration r : registrations) {
            regDocs.add(r.toDocument());
        }
        List<Document> waitDocs = new ArrayList<>();
        for (Registration r : waitingList) {
            waitDocs.add(r.toDocument());
        }
        return new Document("title", title)
                .append("eventType", eventType)
                .append("date", date)
                .append("location", location)
                .append("capacity", capacity)
                .append("status", status)
                .append("registrations", regDocs)
                .append("waitingList", waitDocs);
    }
}