package com.npo.event;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.mongodb.client.model.Filters;
import com.npo.common.DatabaseConnection;
import com.npo.ticket.BasicTicket;
import com.npo.ticket.ITicket;
import com.npo.ticket.MealDecorator;
import com.npo.ticket.VIPDecorator;
import com.npo.user.Admin;
import com.npo.user.Beneficiary;
import com.npo.user.Donor;
import com.npo.user.User;
import com.npo.user.Volunteer;

/**
 * Controller (MVC) for events. View: React EventPage (JSON over REST).
 * Decorator: buildTicket() wraps a BasicTicket with the extras the user chose.
 * Observer: register / changeStatus on Event notify the attendees.
 */
@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "http://localhost:5173")
public class EventController {

    private static final double BASE_PRICE = 100;     // price of a basic ticket

    // POST /api/events   body: { title, eventType, date: "2026-12-01", location, capacity }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createEvent(@RequestBody Map<String, Object> data) {
        Event e = new Event(text(data, "title"), text(data, "eventType"), date(data, "date"),
                text(data, "location"), number(data, "capacity"));
        e.create();
        return toResponse(e);
    }

    // GET /api/events
    @GetMapping
    public List<Map<String, Object>> listEvents() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Document doc : DatabaseConnection.getInstance().select("events", Filters.empty())) {
            result.add(toResponse(loadEvent(doc.getObjectId("_id").toHexString())));
        }
        return result;
    }

    // GET /api/events/{id}
    @GetMapping("/{id}")
    public Map<String, Object> getEvent(@PathVariable String id) {
        return toResponse(loadEvent(id));
    }

    // PUT /api/events/{id}   body: { title, date, location, capacity }
    @PutMapping("/{id}")
    public Map<String, Object> editEvent(@PathVariable String id, @RequestBody Map<String, Object> data) {
        Event e = loadEvent(id);
        e.setTitle(text(data, "title"));
        e.setDate(date(data, "date"));
        e.setLocation(text(data, "location"));
        e.setCapacity(number(data, "capacity"));
        e.edit();
        return toResponse(e);
    }

    // DELETE /api/events/{id}   (attendees are notified that it was cancelled)
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEvent(@PathVariable String id) {
        loadEvent(id).delete();
    }

    // POST /api/events/{id}/register   body: { userId, vip: true/false, mealType: "Vegetarian" or "" }
    @PostMapping("/{id}/register")
    public Map<String, Object> registerAttendee(@PathVariable String id, @RequestBody Map<String, Object> data) {
        Event e = loadEvent(id);
        User user = loadUser(text(data, "userId"));
        ITicket ticket = buildTicket(data);                       // Decorator

        if (e.isFull()) {                                         // client requirement: waiting list
            if (!e.joinWaitingList(user, ticket)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Event is full and the user is already registered or waiting");
            }
            return toResponse(e);
        }

        if (!e.register(user, ticket)) {                          // Observer: attendee is attached + notified
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Registration refused: event not upcoming, or user already registered");
        }
        return toResponse(e);
    }

    // DELETE /api/events/{id}/register/{userId}
    @DeleteMapping("/{id}/register/{userId}")
    public Map<String, Object> cancelRegistration(@PathVariable String id, @PathVariable String userId) {
        Event e = loadEvent(id);
        e.cancelRegistration(loadUser(userId));
        return toResponse(e);
    }

    // PUT /api/events/{id}/status   body: { status }   (all attendees are notified)
    @PutMapping("/{id}/status")
    public Map<String, Object> changeStatus(@PathVariable String id, @RequestBody Map<String, Object> data) {
        String status = text(data, "status");
        if (!status.equals("UPCOMING") && !status.equals("ONGOING")
                && !status.equals("COMPLETED") && !status.equals("CANCELLED")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown status: " + status);
        }
        Event e = loadEvent(id);
        e.changeStatus(status);
        return toResponse(e);
    }

    // GET /api/events/{id}/attendees
    @GetMapping("/{id}/attendees")
    public List<Map<String, Object>> listAttendees(@PathVariable String id) {
        return registrationsOf(loadEvent(id));
    }

    // ===================== helpers =====================

    /** Decorator pattern: start from a basic ticket and wrap it with the chosen extras. */
    private static ITicket buildTicket(Map<String, Object> data) {
        ITicket ticket = new BasicTicket(BASE_PRICE, "Basic ticket");
        if (Boolean.TRUE.equals(data.get("vip"))) {
            ticket = new VIPDecorator(ticket);
        }
        Object meal = data.get("mealType");
        if (meal != null && !meal.toString().trim().isEmpty()) {
            ticket = new MealDecorator(ticket, meal.toString().trim());
        }
        return ticket;
    }

    private static Event loadEvent(String id) {
        Event e = new Event().read(id);
        if (e == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found");
        }
        return e;
    }

    /** Loads a user by id as the right subclass (the role is read first). */
    private static User loadUser(String id) {
        Document doc = DatabaseConnection.getInstance().selectById("users", id);
        if (doc == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        String role = doc.getString("role");
        User user;
        if (role.equals("DONOR")) {
            user = new Donor();
        } else if (role.equals("VOLUNTEER")) {
            user = new Volunteer();
        } else if (role.equals("BENEFICIARY")) {
            user = new Beneficiary();
        } else {
            user = new Admin();
        }
        user.read(id);
        return user;
    }

    /** Event -> JSON. */
    private static Map<String, Object> toResponse(Event e) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", e.getId());
        out.put("title", e.getTitle());
        out.put("eventType", e.getEventType());
        out.put("date", e.getDate());
        out.put("location", e.getLocation());
        out.put("capacity", e.getCapacity());
        out.put("status", e.getStatus());
        out.put("registrations", registrationsOf(e));
        List<Map<String, Object>> waiting = new ArrayList<>();
        for (User u : e.getWaitingList()) {
            Map<String, Object> w = new LinkedHashMap<>();
            w.put("userId", u.getId());
            w.put("name", u.getName());
            waiting.add(w);
        }
        out.put("waitingList", waiting);
        return out;
    }

    private static List<Map<String, Object>> registrationsOf(Event e) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Registration r : e.getRegistrations()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("userId", r.getAttendee().getId());
            m.put("name", r.getAttendee().getName());
            m.put("role", r.getAttendee().getRole());
            m.put("ticket", r.getTicket().getDescription());
            m.put("price", r.getTicket().getPrice());
            m.put("attended", r.isAttended());
            list.add(m);
        }
        return list;
    }

    private static String text(Map<String, Object> data, String key) {
        Object v = data.get(key);
        if (v == null || v.toString().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing field: " + key);
        }
        return v.toString().trim();
    }

    private static int number(Map<String, Object> data, String key) {
        return Integer.parseInt(text(data, key));
    }

    /** "2026-12-01" -> Date */
    private static Date date(Map<String, Object> data, String key) {
        LocalDate d = LocalDate.parse(text(data, key));
        return Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    /** Bad id, bad number or bad date becomes a clean 400 instead of a 500. */
    @ExceptionHandler({IllegalArgumentException.class, java.time.format.DateTimeParseException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String badInput(RuntimeException e) {
        return "Invalid input: " + e.getMessage();
    }

    @DeleteMapping("/{id}/waitlist/{userId}")
    public Map<String, Object> leaveWaitingList(@PathVariable String id, @PathVariable String userId) {
        Event e = loadEvent(id);
        e.leaveWaitingList(loadUser(userId));
        return toResponse(e);
    }

    @PutMapping("/{id}/attend/{userId}")
   public Map<String, Object> markAttended(@PathVariable String id, @PathVariable String userId) {
       Event e = loadEvent(id);
       for (Registration r : e.getRegistrations()) {
           if (r.getAttendee().getId().equals(userId)) {
               r.markAttended();
           }
       }
       e.edit();
       return toResponse(e);
   }
}