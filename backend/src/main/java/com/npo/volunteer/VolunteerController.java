package com.npo.volunteer;

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
import com.npo.user.Volunteer;

/**
 * Controller (MVC) for volunteer tasks and hours. View: React VolunteerPage (JSON over REST).
 * Observer: assigning a volunteer or changing a task's date/location notifies the assignees.
 */
@RestController
@RequestMapping("/api/tasks")
@CrossOrigin(origins = "http://localhost:5173")
public class VolunteerController {

    // POST /api/tasks   body: { title, date: "2026-12-01", location }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createTask(@RequestBody Map<String, Object> data) {
        VolunteerTask t = new VolunteerTask(text(data, "title"), date(data, "date"), text(data, "location"));
        t.create();
        return toResponse(t);
    }

    // GET /api/tasks
    @GetMapping
    public List<Map<String, Object>> listTasks() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Document doc : DatabaseConnection.getInstance().select("volunteerTasks", Filters.empty())) {
            result.add(toResponse(loadTask(doc.getObjectId("_id").toHexString())));
        }
        return result;
    }

    // GET /api/tasks/{id}
    @GetMapping("/{id}")
    public Map<String, Object> getTask(@PathVariable String id) {
        return toResponse(loadTask(id));
    }

    // PUT /api/tasks/{id}   body: { date, location }   (assignees are notified of what changed)
    @PutMapping("/{id}")
    public Map<String, Object> editTask(@PathVariable String id, @RequestBody Map<String, Object> data) {
        VolunteerTask t = loadTask(id);
        Date newDate = date(data, "date");
        String newLocation = text(data, "location");
        if (!newDate.equals(t.getDate())) {
            t.changeDate(newDate);
        }
        if (!newLocation.equals(t.getLocation())) {
            t.changeLocation(newLocation);
        }
        return toResponse(t);
    }

    // DELETE /api/tasks/{id}   (assignees are notified that it was cancelled)
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(@PathVariable String id) {
        loadTask(id).delete();
    }

    // POST /api/tasks/{id}/assign   body: { volunteerId }
    @PostMapping("/{id}/assign")
    public Map<String, Object> assignVolunteer(@PathVariable String id, @RequestBody Map<String, Object> data) {
        VolunteerTask t = loadTask(id);
        Volunteer v = loadVolunteer(text(data, "volunteerId"));
        if (isAssigned(t, v.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Volunteer already assigned to this task");
        }
        if (!t.assignVolunteer(v)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Volunteer is unavailable on the task's date");
        }
            return toResponse(t);
    }

    // DELETE /api/tasks/{id}/assign/{volunteerId}
    @DeleteMapping("/{id}/assign/{volunteerId}")
    public Map<String, Object> removeVolunteer(@PathVariable String id, @PathVariable String volunteerId) {
        VolunteerTask t = loadTask(id);
        t.removeVolunteer(loadVolunteer(volunteerId));
        return toResponse(t);
    }

    // GET /api/tasks/{id}/assignees
    @GetMapping("/{id}/assignees")
    public List<Map<String, Object>> listAssignees(@PathVariable String id) {
        return assigneesOf(loadTask(id));
    }

    // POST /api/tasks/hours   body: { volunteerId, taskId, hours }
    @PostMapping("/hours")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> logHours(@RequestBody Map<String, Object> data) {
        Volunteer v = loadVolunteer(text(data, "volunteerId"));
        VolunteerTask t = loadTask(text(data, "taskId"));
        double hours = Double.parseDouble(text(data, "hours"));
        if (hours <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hours must be greater than 0");
        }
        if (!isAssigned(t, v.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Volunteer is not assigned to this task");
        }
        HoursLog log = new HoursLog(v, t, hours);
        log.logHours();
        return hoursResponse(log);
    }

    // GET /api/tasks/hours/{volunteerId}   (all hours logged by one volunteer)
    @GetMapping("/hours/{volunteerId}")
    public List<Map<String, Object>> listHours(@PathVariable String volunteerId) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Document doc : DatabaseConnection.getInstance().select("hoursLogs", Filters.eq("volunteerId", volunteerId))) {
            HoursLog log = new HoursLog().read(doc.getObjectId("_id").toHexString());
            result.add(hoursResponse(log));
        }
        return result;
    }

    // DELETE /api/tasks/hours/log/{logId}   (removes a wrong entry and its hours)
    @DeleteMapping("/hours/log/{logId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteHours(@PathVariable String logId) {
        HoursLog log = new HoursLog().read(logId);
        if (log == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Hours log not found");
        }
        log.delete();
    }

    // ===================== helpers =====================

    private static VolunteerTask loadTask(String id) {
        VolunteerTask t = new VolunteerTask().read(id);
        if (t == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found");
        }
        return t;
    }

    /** Loads a user only if it really is a volunteer. */
    private static Volunteer loadVolunteer(String id) {
        Document doc = DatabaseConnection.getInstance().selectById("users", id);
        if (doc == null || !"VOLUNTEER".equals(doc.getString("role"))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Volunteer not found");
        }
        return new Volunteer().read(id);
    }

    private static boolean isAssigned(VolunteerTask t, String volunteerId) {
        for (Volunteer a : t.getAssignees()) {
            if (a.getId().equals(volunteerId)) {
                return true;
            }
        }
        return false;
    }

    /** VolunteerTask -> JSON. */
    private static Map<String, Object> toResponse(VolunteerTask t) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", t.getId());
        out.put("title", t.getTitle());
        out.put("date", t.getDate());
        out.put("location", t.getLocation());
        out.put("assignees", assigneesOf(t));
        return out;
    }

    private static List<Map<String, Object>> assigneesOf(VolunteerTask t) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Volunteer v : t.getAssignees()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", v.getId());
            m.put("name", v.getName());
            m.put("availability", v.getAvailability());
            m.put("totalHours", v.getHours());
            list.add(m);
        }
        return list;
    }

    private static Map<String, Object> hoursResponse(HoursLog log) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", log.getId());
        out.put("volunteerId", log.getVolunteer().getId());
        out.put("volunteerName", log.getVolunteer().getName());
        out.put("taskId", log.getTask().getId());
        out.put("taskTitle", log.getTask().getTitle());
        out.put("hours", log.getHours());
        out.put("date", log.getDate());
        return out;
    }

    private static String text(Map<String, Object> data, String key) {
        Object v = data.get(key);
        if (v == null || v.toString().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing field: " + key);
        }
        return v.toString().trim();
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

    // POST /api/tasks/unavailable/{volunteerId}   body: { "date": "2026-12-01" }
    @PostMapping("/unavailable/{volunteerId}")
    public List<Date> addUnavailableDate(@PathVariable String volunteerId, @RequestBody Map<String, Object> data) {
        Volunteer v = loadVolunteer(volunteerId);
        v.addUnavailableDate(date(data, "date"));
        return v.getUnavailableDates();
    }

    // DELETE /api/tasks/unavailable/{volunteerId}/2026-12-01
    @DeleteMapping("/unavailable/{volunteerId}/{date}")
    public List<Date> removeUnavailableDate(@PathVariable String volunteerId, @PathVariable String date) {
        Volunteer v = loadVolunteer(volunteerId);
        v.removeUnavailableDate(date(Map.of("date", date), "date"));
        return v.getUnavailableDates();
    }

    @GetMapping("/unavailable/{volunteerId}")
    public List<Date> listUnavailableDates(@PathVariable String volunteerId) {
        return loadVolunteer(volunteerId).getUnavailableDates();
    }
}