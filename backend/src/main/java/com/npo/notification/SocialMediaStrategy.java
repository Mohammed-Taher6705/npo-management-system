package com.npo.notification;

import com.npo.user.User;

/** Concrete Strategy: delivers as a social-media direct message (simulated in Phase 1). */
public class SocialMediaStrategy implements ICommunicationStrategy {
    private final String platform = "Facebook";
    private final String accessToken = "";

    @Override
    public boolean send(User to, String msg) {
        return validateRecipient(to);   // real platform API call would go here
    }

    @Override
    public String formatMessage(String msg) {
        return "[NPO] " + msg;
    }

    @Override
    public boolean validateRecipient(User to) {
        return to.getName() != null && !to.getName().isBlank();
    }

    public String getPlatform() { return platform; }
}
