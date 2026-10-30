package com.npo.event;
import java.util.Date;

import org.bson.Document;

import com.npo.ticket.BasicTicket;
import com.npo.ticket.ITicket;
import com.npo.user.Admin;
import com.npo.user.Beneficiary;
import com.npo.user.Donor;
import com.npo.user.User;
import com.npo.user.Volunteer;

public class Registration {
    private User attendee;
    private ITicket ticket;
    private Date regDate;
    private boolean attended;

    public Registration(User attendee, ITicket ticket) {
        this.attendee = attendee;
        this.ticket = ticket;
        this.regDate = new Date();
        this.attended = false;
    }
    public Registration() {
    }

    public void markAttended() {
        attended = true;
    }

    public User getAttendee() { return attendee; }
    public ITicket getTicket() { return ticket; }
    public Date getRegDate() { return regDate; }
    public boolean isAttended() { return attended; }

    public Document toDocument() {
        return new Document("userId", attendee.getId())
                .append("role", attendee.getRole())
                .append("ticketDescription", ticket.getDescription())
                .append("ticketPrice", ticket.getPrice())
                .append("regDate", regDate)
                .append("attended", attended);
    }

    public void load(Document doc) {
        regDate = doc.getDate("regDate");
        attended = doc.getBoolean("attended");

        // rebuild the right type of user from the saved role
        String role = doc.getString("role");
        if (role.equals("DONOR")) {
            attendee = new Donor();
        } else if (role.equals("VOLUNTEER")) {
            attendee = new Volunteer();
        } else if (role.equals("BENEFICIARY")) {
            attendee = new Beneficiary();
        } else {
            attendee = new Admin();
        }
        attendee.read(doc.getString("userId"));

        // the final ticket (already decorated when bought) is kept as its price + description
        ticket = new BasicTicket(doc.getDouble("ticketPrice"), doc.getString("ticketDescription"));
    }

    
}
