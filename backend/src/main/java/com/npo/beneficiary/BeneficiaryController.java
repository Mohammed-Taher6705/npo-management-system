package com.npo.beneficiary;

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
import com.npo.lookup.NeedCategory;
import com.npo.user.Beneficiary;

/**
 * Controller (MVC) for beneficiaries and their needs. View: React BeneficiaryPage (JSON over REST).
 * Need categories come from the NeedCategory lookup table.
 */
@RestController
@RequestMapping("/api/beneficiaries")
@CrossOrigin(origins = "http://localhost:5173")
public class BeneficiaryController {

    // POST /api/beneficiaries
    // body: { name, email, phone, password, preferredChannel, address, familySize }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> addBeneficiary(@RequestBody Map<String, Object> data) {
        String email = text(data, "email");
        if (!DatabaseConnection.getInstance().select("users", Filters.eq("email", email)).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already registered");
        }
        Beneficiary b = new Beneficiary(text(data, "name"), email, text(data, "phone"),
                text(data, "password"), text(data, "preferredChannel"),
                text(data, "address"), number(data, "familySize"));
        b.create();
        return toResponse(b);
    }

    // GET /api/beneficiaries
    @GetMapping
    public List<Map<String, Object>> listBeneficiaries() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Document doc : DatabaseConnection.getInstance().select("users", Filters.eq("role", "BENEFICIARY"))) {
            result.add(toResponse(loadBeneficiary(doc.getObjectId("_id").toHexString())));
        }
        return result;
    }

    // GET /api/beneficiaries/{id}
    @GetMapping("/{id}")
    public Map<String, Object> viewBeneficiary(@PathVariable String id) {
        return toResponse(loadBeneficiary(id));
    }

    // PUT /api/beneficiaries/{id}   body: { name, email, phone, preferredChannel, address, familySize }
    @PutMapping("/{id}")
    public Map<String, Object> editBeneficiary(@PathVariable String id, @RequestBody Map<String, Object> data) {
        Beneficiary b = loadBeneficiary(id);
        b.setName(text(data, "name"));
        b.setEmail(text(data, "email"));
        b.setPhone(text(data, "phone"));
        b.setPreferredChannel(text(data, "preferredChannel"));
        b.setAddress(text(data, "address"));
        b.setFamilySize(number(data, "familySize"));
        b.edit();
        return toResponse(b);
    }

    // DELETE /api/beneficiaries/{id}   (their needs are deleted too)
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBeneficiary(@PathVariable String id) {
        Beneficiary b = loadBeneficiary(id);
        for (Need n : b.getNeeds()) {
            n.delete();
        }
        b.delete();
    }

    // POST /api/beneficiaries/{id}/needs   body: { categoryId, priority: "LOW" | "MEDIUM" | "HIGH" }
    @PostMapping("/{id}/needs")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> addNeed(@PathVariable String id, @RequestBody Map<String, Object> data) {
        Beneficiary b = loadBeneficiary(id);
        NeedCategory category = new NeedCategory();
        if (category.read(text(data, "categoryId")) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Need category not found");
        }
        Need n = new Need(category, priority(data));
        n.create();                       // must be saved first so it has an id
        b.addNeed(n);
        return toResponse(b);
    }

    // PUT /api/beneficiaries/{id}/needs/{needId}   body: { priority, status: "OPEN" | "FULFILLED" }
    @PutMapping("/{id}/needs/{needId}")
    public Map<String, Object> updateNeed(@PathVariable String id, @PathVariable String needId,
                                          @RequestBody Map<String, Object> data) {
        Beneficiary b = loadBeneficiary(id);
        Need n = findNeed(b, needId);
        String status = text(data, "status");
        if (!status.equals("OPEN") && !status.equals("FULFILLED")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status must be OPEN or FULFILLED");
        }
        boolean wasUrgent = n.isUrgent();
        n.setPriority(priority(data));
        n.setStatus(status);
        n.edit();
        if (!wasUrgent && n.isUrgent()) {
            n.announceUrgent(b);
        }
        return toResponse(loadBeneficiary(id));
    }

    // DELETE /api/beneficiaries/{id}/needs/{needId}
    @DeleteMapping("/{id}/needs/{needId}")
    public Map<String, Object> removeNeed(@PathVariable String id, @PathVariable String needId) {
        Beneficiary b = loadBeneficiary(id);
        Need n = findNeed(b, needId);
        b.removeNeed(n);
        n.delete();
        return toResponse(b);
    }

    // GET /api/beneficiaries/{id}/needs
    @GetMapping("/{id}/needs")
    public List<Map<String, Object>> listNeeds(@PathVariable String id) {
        return needsOf(loadBeneficiary(id));
    }

    // ===================== helpers =====================

    /** Loads a user only if it really is a beneficiary. */
    private static Beneficiary loadBeneficiary(String id) {
        Document doc = DatabaseConnection.getInstance().selectById("users", id);
        if (doc == null || !"BENEFICIARY".equals(doc.getString("role"))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Beneficiary not found");
        }
        return new Beneficiary().read(id);
    }

    /** Finds a need that belongs to this beneficiary. */
    private static Need findNeed(Beneficiary b, String needId) {
        for (Need n : b.getNeeds()) {
            if (n.getId().equals(needId)) {
                return n;
            }
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Need not found for this beneficiary");
    }

    private static String priority(Map<String, Object> data) {
        String p = text(data, "priority");
        if (!p.equals("LOW") && !p.equals("MEDIUM") && !p.equals("HIGH")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Priority must be LOW, MEDIUM or HIGH");
        }
        return p;
    }

    /** Beneficiary -> JSON (never includes the password). */
    private static Map<String, Object> toResponse(Beneficiary b) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", b.getId());
        out.put("name", b.getName());
        out.put("email", b.getEmail());
        out.put("phone", b.getPhone());
        out.put("preferredChannel", b.getPreferredChannel());
        out.put("address", b.getAddress());
        out.put("familySize", b.getFamilySize());
        out.put("needs", needsOf(b));
        return out;
    }

    private static List<Map<String, Object>> needsOf(Beneficiary b) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Need n : b.getNeeds()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", n.getId());
            m.put("categoryId", n.getCategory().getId());
            m.put("category", n.getCategory().getName());
            m.put("priority", n.getPriority());
            m.put("status", n.getStatus());
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

    /** Bad id or bad number becomes a clean 400 instead of a 500. */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String badInput(IllegalArgumentException e) {
        return "Invalid input: " + e.getMessage();
    }
}