package com.npo.user;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.client.model.Filters;
import com.npo.common.DatabaseConnection;

public class Admin extends User{
        public Admin() {
        role = "ADMIN";
    }

    public Admin(String name, String email, String phone, String password, String preferredChannel) {
        super(name, email, phone, password, preferredChannel, "ADMIN");
    }

    // admin-only: see every user in the system
    public List<Document> getAllUsers() {
        return DatabaseConnection.getInstance().select("users", Filters.empty());
    }
    
}