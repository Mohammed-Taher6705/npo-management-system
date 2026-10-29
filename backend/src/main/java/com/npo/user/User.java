package com.npo.user;

import com.npo.common.DatabaseConnection;
import com.npo.common.IObserver;
import com.npo.common.ISubject;
import com.npo.notification.EmailStrategy;
import com.npo.notification.SMSStrategy;
import com.npo.notification.SocialMediaStrategy;
import com.npo.notification.Notification;

import org.bson.Document;

/**
 * Base class for every person in the system (Donor, Volunteer, Beneficiary, Admin).
 * Observer pattern: a User is notified when an Event or VolunteerTask it follows changes.
 * All users are stored in one MongoDB collection ("users"); the "role" field tells them apart.
 */
public abstract class User implements IObserver {
    protected String id;          
    protected String name;
    protected String email;
    protected String phone;
    protected String password;
    protected String preferredChannel;
    protected String role;
    protected boolean loggedIn = false;

    public User(String name, String email, String phone, String password, String preferredChannel, String role) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.password = password;
        this.preferredChannel = preferredChannel;
        this.role = role;
    }

    public User() {
    }

    public void create() {
        Document data = new Document("role", getRole())
                .append("name", name)
                .append("email", email)
                .append("phone", phone)
                .append("password", password)
                .append("preferredChannel", preferredChannel);

        id = DatabaseConnection.getInstance().insert("users", data);
    }

    public User read(String id) {
        Document doc = DatabaseConnection.getInstance().selectById("users", id);
        if (doc == null) {
            return null;                       
        }

        this.id = id;
        this.role = doc.getString("role");
        this.name = doc.getString("name");
        this.email = doc.getString("email");
        this.phone = doc.getString("phone");
        this.password = doc.getString("password");
        this.preferredChannel = doc.getString("preferredChannel");
        return this;
    }

    public void edit() {
        Document data = new Document("name", name)
                .append("email", email)
                .append("phone", phone)
                .append("password", password)
                .append("preferredChannel", preferredChannel);

        DatabaseConnection.getInstance().update("users", id, data);
    }

    public void delete() {
        DatabaseConnection.getInstance().delete("users", id);
    }

    public boolean login(String email, String password) {
        Document doc = DatabaseConnection.getInstance().getCollection("users")
                .find(new Document("email", email).append("password", password)).first();
        if (doc == null) {
            return false;                      
        }

        this.id = doc.getObjectId("_id").toHexString();
        this.role = doc.getString("role");
        this.name = doc.getString("name");
        this.email = doc.getString("email");
        this.phone = doc.getString("phone");
        this.password = doc.getString("password");
        this.preferredChannel = doc.getString("preferredChannel");
        this.loggedIn = true;
        return true;
    }

    public void logout() {
        this.loggedIn = false;
    }

    public void changePassword(String oldPassword, String newPassword) {
        if (this.password.equals(oldPassword)) {
            this.password = newPassword;
            edit();
        }
    }

    public String getName(){
        return name;
    }

    public String getEmail(){
        return email;
    }

    public String getPhone(){
        return phone;
    }

    public void setPreferredChannel(String preferredChannel) {
        this.preferredChannel = preferredChannel;
    }

    @Override
    public void update(ISubject s) {
        Notification notification = new Notification(this, s.getUpdateMessage());

        if (preferredChannel.equals("SMS")) {
            notification.setStrategy(new SMSStrategy());
        } else if (preferredChannel.equals("SOCIAL")) {
            notification.setStrategy(new SocialMediaStrategy());
        } else {
            notification.setStrategy(new EmailStrategy());
        }

        notification.send();
    }

    public String getRole() {
        return role;
    }

    public String getId() {
        return id;
    }

}
