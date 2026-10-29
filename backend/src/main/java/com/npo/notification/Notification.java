package com.npo.notification;

import java.util.Date;
import com.npo.common.DatabaseConnection;
import com.npo.user.User;

import org.bson.Document;


/**
 * Strategy pattern context: holds the chosen ICommunicationStrategy and uses it to send.
 * Every notification is saved in the "notifications" collection so the React UI can show it.
 */
public class Notification {
    private  User recipient;
    private  String content;
    private  Date date = new Date();
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
        String text = strategy.formatMessage(content);       
        boolean sent = strategy.send(recipient, text);        

        if (sent) {                                           
            status = "SENT";
        } else {
            status = "FAILED";
        }

        Document data = new Document("recipientId", recipient.getId())   
                .append("content", text)
                .append("date", date)
                .append("status", status);
        DatabaseConnection.getInstance().insert("notifications", data);

        return sent;
    }



    public String getStatus() { return status; }
}
