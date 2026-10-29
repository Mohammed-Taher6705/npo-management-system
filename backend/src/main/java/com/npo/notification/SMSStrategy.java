package com.npo.notification;

import com.npo.user.User;

/** Concrete Strategy: delivers by SMS (simulated in Phase 1). */
public class SMSStrategy implements ICommunicationStrategy {
    private final String provider = "LocalSMSGateway";

    @Override
    public boolean send(User to, String msg) {
        return validateRecipient(to);   // real SMS gateway call would go here
    }

    @Override
    public String formatMessage(String msg) {
        return msg.length() <= 160 ? msg : msg.substring(0, 157) + "...";   // one SMS = 160 chars
    }

    @Override
    public boolean validateRecipient(User to) {
        return to.getPhone() != null && to.getPhone().matches("\\+?[0-9 ]{8,15}");
    }

    public String getProvider() { return provider; }
}
