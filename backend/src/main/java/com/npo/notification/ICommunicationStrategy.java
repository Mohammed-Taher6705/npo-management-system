package com.npo.notification;

import com.npo.user.User;

/** Strategy pattern: one way of delivering a message to a user. */
public interface ICommunicationStrategy {
    boolean send(User to, String msg);
    String formatMessage(String msg);
    boolean validateRecipient(User to);
}
