package com.npo.donation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.bson.types.ObjectId;
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
import com.npo.payment.CashPayment;
import com.npo.payment.CheckPayment;
import com.npo.payment.IPaymentMethod;
import com.npo.payment.OnlinePayment;
import com.npo.user.Donor;

/**
 * Controller (MVC) for donations. View: React DonationPage (JSON over REST).
 * Factory Method: creatorFor() picks MoneyDonationCreator or InKindDonationCreator.
 * Strategy: paymentFor() builds the payment method and gives it to the MoneyDonation (context).
 */
@RestController
@RequestMapping("/api/donations")
@CrossOrigin(origins = "http://localhost:5173")
public class DonationController {

    private static final String COLLECTION = "donations";

    // ===================================================================
    // Endpoints
    // ===================================================================

    // GET /api/donations
    @GetMapping
    public List<Map<String, Object>> listDonations() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Document doc : DatabaseConnection.getInstance().select(COLLECTION, Filters.empty())) {
            result.add(toResponse(doc));
        }
        return result;
    }

    // GET /api/donations/{id}
    @GetMapping("/{id}")
    public Map<String, Object> getDonation(@PathVariable String id) {
        return toResponse(findDocument(id));
    }

    // POST /api/donations
    // body (money):   { type: "money", donorId, amount, currency, paymentMethod, receivedBy | checkNo+bankName | cardHolder }
    // body (in-kind): { type: "in-kind", donorId, itemName, quantity, estimatedValue }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> donate(@RequestBody Map<String, Object> data) {
        Donor donor = loadDonor(text(data, "donorId"));
        DonationCreator creator = creatorFor(data);
        IPaymentMethod payment = paymentFor(data);             // Factory Method
        Donation donation = creator.registerDonation(donor);

        if (donation instanceof MoneyDonation) {                     // Strategy
            
            if (payment != null) {                                   // null = pay later
                MoneyDonation money = (MoneyDonation) donation;
                money.setPaymentMethod(payment);
                money.processPayment();
            }
        }
        return toResponse(findDocument(donation.getId()));
    }

    // POST /api/donations/{id}/pay   body: { paymentMethod, receivedBy | checkNo+bankName | cardHolder }
    @PostMapping("/{id}/pay")
    public Map<String, Object> pay(@PathVariable String id, @RequestBody Map<String, Object> data) {
        Donation donation = loadDonation(id);
        if (!(donation instanceof MoneyDonation)) {
            throw badRequest("Only money donations can be paid");
        }
        IPaymentMethod payment = paymentFor(data);
        if (payment == null) {
            throw badRequest("Choose a payment method");
        }
        MoneyDonation money = (MoneyDonation) donation;
        money.setPaymentMethod(payment);
        money.processPayment();
        return toResponse(findDocument(id));
    }

    // PUT /api/donations/{id}   body: { status }   (only the status can change)
    @PutMapping("/{id}")
    public Map<String, Object> editDonation(@PathVariable String id,
                                            @RequestBody Map<String, Object> data) {
        Donation donation = loadDonation(id);
        donation.setStatus(text(data, "status"));
        donation.edit();
        return toResponse(findDocument(id));
    }

    // DELETE /api/donations/{id}
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDonation(@PathVariable String id) {
        loadDonation(id).delete();
    }

    // ===================================================================
    // Pattern helpers
    // ===================================================================

    /** Factory Method: choose the creator from the request. */
    private static DonationCreator creatorFor(Map<String, Object> data) {
        String type = text(data, "type").toLowerCase();
        switch (type) {
            case "money": {
                double amount = number(data, "amount");
                if (amount <= 0) throw badRequest("Amount must be greater than 0");
                return new MoneyDonationCreator(amount, text(data, "currency"));
            }
            case "in-kind": {
                int quantity = (int) number(data, "quantity");
                double value = number(data, "estimatedValue");
                if (quantity <= 0) throw badRequest("Quantity must be at least 1");
                if (value < 0) throw badRequest("Estimated value cannot be negative");
                return new InKindDonationCreator(text(data, "itemName"), quantity, value);
            }
            default:
                throw badRequest("Unknown donation type: " + type + " (use money or in-kind)");
        }
    }

    /** Strategy pattern: build the payment method the user chose (null = pay later). */
    private static IPaymentMethod paymentFor(Map<String, Object> data) {
        Object method = data.get("paymentMethod");
        if (method == null || method.toString().isEmpty()) {
            return null;
        }
        switch (method.toString()) {
            case "cash":   return new CashPayment(text(data, "receivedBy"));
            case "check":  return new CheckPayment(text(data, "checkNo"), text(data, "bankName"));
            case "online": return new OnlinePayment(text(data, "cardHolder"));
            default: throw badRequest("Unknown payment method: " + method);
        }
    }

    // ===================================================================
    // Loading helpers
    // ===================================================================

    private static Donor loadDonor(String donorId) {
        Donor donor = new Donor();
        if (donor.read(donorId) == null) {
            throw notFound("Donor not found");
        }
        return donor;
    }

    /** Empty subclass instance for the stored "type" value (MONEY or IN_KIND). */
    private static Donation emptyDonation(String storedType) {
        String t = storedType == null ? "" : storedType.toLowerCase();
        if (t.contains("money")) return new MoneyDonation();
        if (t.contains("kind")) return new InKindDonation();
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Unknown donation type stored in database: " + storedType);
    }

    private static Document findDocument(String id) {
        Document doc = DatabaseConnection.getInstance().selectById(COLLECTION, id);
        if (doc == null) throw notFound("Donation not found");
        return doc;
    }

    private static Donation loadDonation(String id) {
        Document doc = findDocument(id);
        Donation donation = emptyDonation(doc.getString("type")).read(id);
        if (donation == null) throw notFound("Donation not found");
        return donation;
    }

    // ===================================================================
    // JSON helpers
    // ===================================================================

    /** Mongo document to JSON-friendly map (_id becomes id, ObjectIds become strings). */
    private static Map<String, Object> toResponse(Document doc) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : doc.entrySet()) {
            if (e.getKey().equals("_id")) {
                out.put("id", clean(e.getValue()));
            } else {
                out.put(e.getKey(), clean(e.getValue()));
            }
        }
        return out;
    }

    private static Object clean(Object value) {
        if (value instanceof ObjectId) return ((ObjectId) value).toHexString();
        if (value instanceof Document) return toResponse((Document) value);
        if (value instanceof List) {
            List<Object> list = new ArrayList<>();
            for (Object o : (List<?>) value) list.add(clean(o));
            return list;
        }
        return value;
    }

    private static String text(Map<String, Object> data, String key) {
        Object v = data.get(key);
        if (v == null || v.toString().trim().isEmpty()) {
            throw badRequest("Missing field: " + key);
        }
        return v.toString().trim();
    }

    private static double number(Map<String, Object> data, String key) {
        Object v = data.get(key);
        if (v instanceof Number) return ((Number) v).doubleValue();
        try {
            return Double.parseDouble(text(data, key));
        } catch (NumberFormatException e) {
            throw badRequest("Field must be a number: " + key);
        }
    }

    private static ResponseStatusException badRequest(String msg) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }

    private static ResponseStatusException notFound(String msg) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, msg);
    }

    /** A malformed MongoDB id becomes a clean 400 instead of a 500. */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String invalidId(IllegalArgumentException e) {
        return "Invalid id or data: " + e.getMessage();
    }
}
