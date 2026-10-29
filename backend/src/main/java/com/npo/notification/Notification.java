package com.npo.notification;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.npo.common.DatabaseConnection;
import com.npo.user.User;

/**
 * Strategy pattern context: holds the chosen ICommunicationStrategy and uses it to send.
 * Every notification is saved in the "notifications" collection so the React UI can show it.
 */
public class Notification {
    private final User recipient;
    private final String content;
    private final Date date = new Date();
    private String status = "PENDING";
    private ICommunicationStrategy strategy;

    public Notification(User recipient, String content) {
        this.recipient = recipient;
        this.content = content;
    }

    public void setStrategy(ICommunicationStrategy s) {
        this.strategy = s;
    }

    public boolean send() {
        if (strategy == null) throw new IllegalStateException("No communication strategy set");
        String text = strategy.formatMessage(content);
        boolean ok = strategy.send(recipient, text);
        status = ok ? "SENT" : "FAILED";

        Map<String, Object> data = new HashMap<>();
        data.put("recipientId", recipient.getId());
        data.put("channel", strategy.getClass().getSimpleName());
        data.put("content", text);
        data.put("date", date);
        data.put("status", status);
        DatabaseConnection.getInstance().insert("notifications", data);
        return ok;
    }

    public String getStatus() { return status; }
}
