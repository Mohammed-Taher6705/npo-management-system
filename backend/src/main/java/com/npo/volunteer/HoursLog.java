package com.npo.volunteer;

import java.util.Date;

import org.bson.Document;

import com.npo.common.DatabaseConnection;
import com.npo.user.Volunteer;

public class HoursLog {
    private String id;
    private Volunteer volunteer;
    private VolunteerTask task;
    private double hours;
    private Date date;

    public HoursLog() {
    }

    public HoursLog(Volunteer volunteer, VolunteerTask task, double hours) {
        this.volunteer = volunteer;
        this.task = task;
        this.hours = hours;
        this.date = new Date();          // today
    }

    // saves the log and adds the hours to the volunteer's total
    public void logHours() {
        Document data = new Document("volunteerId", volunteer.getId())
                .append("taskId", task.getId())
                .append("hours", hours)
                .append("date", date);

        id = DatabaseConnection.getInstance().insert("hoursLogs", data);
        volunteer.addHours(hours);
    }

    public HoursLog read(String id) {
        Document doc = DatabaseConnection.getInstance().selectById("hoursLogs", id);
        if (doc == null) {
            return null;
        }
        this.id = id;
        this.hours = doc.getDouble("hours");
        this.date = doc.getDate("date");

        this.volunteer = new Volunteer();
        this.volunteer.read(doc.getString("volunteerId"));

        this.task = new VolunteerTask();
        this.task.read(doc.getString("taskId"));
        return this;
    }

    public void delete() {
        volunteer.addHours(-hours);      // take the hours back off the total
        DatabaseConnection.getInstance().delete("hoursLogs", id);
    }

    // ----- getters -----
    public String getId() { return id; }
    public Volunteer getVolunteer() { return volunteer; }
    public VolunteerTask getTask() { return task; }
    public double getHours() { return hours; }
    public Date getDate() { return date; }
}
