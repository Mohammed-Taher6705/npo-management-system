package com.npo.user;

import java.util.ArrayList;
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

/**
 * Controller (MVC) for users: the User Control Panel.
 * View: React UserPage (JSON over REST). Model: User and its subclasses.
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:5173")
public class UserController {

    // POST /api/users/register
    // body: { role, name, email, phone, password, preferredChannel,
    //         donorType | availability | address + familySize }
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> register(@RequestBody Map<String, Object> data) {
        String role = text(data, "role");
        String name = text(data, "name");
        String email = text(data, "email");
        String phone = text(data, "phone");
        String password = text(data, "password");
        String channel = text(data, "preferredChannel");

        if (!DatabaseConnection.getInstance().select("users", Filters.eq("email", email)).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already registered");
        }

        User user;
        if (role.equals("DONOR")) {
            user = new Donor(name, email, phone, password, channel, text(data, "donorType"));
        } else if (role.equals("VOLUNTEER")) {
            user = new Volunteer(name, email, phone, password, channel, text(data, "availability"));
        } else if (role.equals("BENEFICIARY")) {
            user = new Beneficiary(name, email, phone, password, channel,
                    text(data, "address"), Integer.parseInt(text(data, "familySize")));
        } else if (role.equals("ADMIN")) {
            user = new Admin(name, email, phone, password, channel);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown role: " + role);
        }
        user.create();
        return toResponse(user);
    }

    // POST /api/users/login   body: { role, email, password }
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, Object> data) {
        User user = emptyUser(text(data, "role"));
        if (!user.login(text(data, "email"), text(data, "password"))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Wrong email or password");
        }
        return toResponse(user);
    }

    // GET /api/users            (admin: list everyone)
    @GetMapping
    public List<Map<String, Object>> listUsers() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Document doc : new Admin().getAllUsers()) {
            result.add(toResponse(loadUser(doc.getObjectId("_id").toHexString())));
        }
        return result;
    }

    // GET /api/users/{id}
    @GetMapping("/{id}")
    public Map<String, Object> getUser(@PathVariable String id) {
        return toResponse(loadUser(id));
    }

    // PUT /api/users/{id}   body: { name, email, phone, preferredChannel }  (edit profile)
    @PutMapping("/{id}")
    public Map<String, Object> editProfile(@PathVariable String id, @RequestBody Map<String, Object> data) {
        User user = loadUser(id);
        user.setName(text(data, "name"));
        user.setEmail(text(data, "email"));
        user.setPhone(text(data, "phone"));
        user.setPreferredChannel(text(data, "preferredChannel"));
        user.edit();
        return toResponse(user);
    }

    // PUT /api/users/{id}/password   body: { oldPassword, newPassword }
    @PutMapping("/{id}/password")
    public Map<String, Object> changePassword(@PathVariable String id, @RequestBody Map<String, Object> data) {
        User user = loadUser(id);
        if (!user.changePassword(text(data, "oldPassword"), text(data, "newPassword"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Old password is incorrect");
        }
        return toResponse(user);
    }

    // DELETE /api/users/{id}
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable String id) {
        loadUser(id).delete();
    }

    // GET /api/users/{id}/notifications   (messages sent to this user by the Observer pattern)
    @GetMapping("/{id}/notifications")
    public List<Map<String, Object>> notifications(@PathVariable String id) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Document doc : DatabaseConnection.getInstance().select("notifications", Filters.eq("recipientId", id))) {
            Map<String, Object> n = new LinkedHashMap<>();
            n.put("content", doc.getString("content"));
            n.put("date", doc.getDate("date"));
            n.put("status", doc.getString("status"));
            result.add(n);
        }
        return result;
    }

    // ===================== helpers =====================

    /** Empty object of the right subclass for a role. */
    private static User emptyUser(String role) {
        if (role.equals("DONOR")) return new Donor();
        if (role.equals("VOLUNTEER")) return new Volunteer();
        if (role.equals("BENEFICIARY")) return new Beneficiary();
        if (role.equals("ADMIN")) return new Admin();
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown role: " + role);
    }

    /** Loads a user by id as the right subclass (the role is read first). */
    private static User loadUser(String id) {
        Document doc = DatabaseConnection.getInstance().selectById("users", id);
        if (doc == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        User user = emptyUser(doc.getString("role"));
        user.read(id);
        return user;
    }

    /** User -> JSON (never includes the password). */
    private static Map<String, Object> toResponse(User user) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", user.getId());
        out.put("role", user.getRole());
        out.put("name", user.getName());
        out.put("email", user.getEmail());
        out.put("phone", user.getPhone());
        out.put("preferredChannel", user.getPreferredChannel());

        if (user instanceof Donor) {
            Donor d = (Donor) user;
            out.put("donorType", d.getDonorType());
            out.put("totalDonated", d.getTotalDonated());
        } else if (user instanceof Volunteer) {
            Volunteer v = (Volunteer) user;
            out.put("availability", v.getAvailability());
            out.put("totalHours", v.getHours());
        } else if (user instanceof Beneficiary) {
            Beneficiary b = (Beneficiary) user;
            out.put("address", b.getAddress());
            out.put("familySize", b.getFamilySize());
        }
        return out;
    }

    private static String text(Map<String, Object> data, String key) {
        Object v = data.get(key);
        if (v == null || v.toString().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing field: " + key);
        }
        return v.toString().trim();
    }

    /** A malformed MongoDB id becomes a clean 400 instead of a 500. */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String invalidId(IllegalArgumentException e) {
        return "Invalid id: " + e.getMessage();
    }
}