package com.npo.notification;

import com.npo.user.User;

/** Concrete Strategy: delivers by email (simulated in Phase 1). */
public class EmailStrategy implements ICommunicationStrategy {

    @Override
    public boolean send(User to, String msg) {
        return validateRecipient(to);   
    }

    @Override
    public String formatMessage(String msg) {
        return "Dear member,\n\n" + msg + "\n\nNPO Team";
    }

    @Override
    public boolean validateRecipient(User to) {
        return to.getEmail() != null && to.getEmail().contains("@");
    }

}
