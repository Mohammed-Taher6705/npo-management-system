package com.npo.notification;

import com.npo.user.User;

/** Concrete Strategy: delivers by email (simulated in Phase 1). */
public class EmailStrategy implements ICommunicationStrategy {
    private final String smtpServer = "smtp.npo.org";

    @Override
    public boolean send(User to, String msg) {
        return validateRecipient(to);   // real SMTP sending would go here
    }

    @Override
    public String formatMessage(String msg) {
        return "Dear member,\n\n" + msg + "\n\nNPO Team";
    }

    @Override
    public boolean validateRecipient(User to) {
        return to.getEmail() != null && to.getEmail().contains("@");
    }

    public String getSmtpServer() { return smtpServer; }
}
