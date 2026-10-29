package com.npo.lookup;

import com.mongodb.client.model.Filters;
import com.npo.common.DatabaseConnection;
import com.npo.lookup.ItemCategory;
import com.npo.lookup.LookupTable;
import com.npo.lookup.NeedCategory;
import com.npo.lookup.Skill;
import org.bson.Document;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller (MVC) for the lookup tables.
 * Model: LookupTable and its subclasses. View: the React LookupPage (talks JSON over REST).
 */
@RestController
@RequestMapping("/api/lookups")
@CrossOrigin(origins = "http://localhost:5173")   // allows the React dev server to call this API
public class LookupController {

    // ---------- GET /api/lookups/{type} ----------
    @GetMapping("/{type}")
    public List<Map<String, Object>> listLookups(@PathVariable String type) {
        LookupTable table = newInstance(type);
        List<Map<String, Object>> result = new ArrayList<>();

        for (Document doc : DatabaseConnection.getInstance()
                .select(table.getCollectionName(), Filters.empty())) {
            Map<String, Object> item = new LinkedHashMap<>(doc);
            item.remove("_id");
            item.put("id", doc.getObjectId("_id").toHexString());
            result.add(item);
        }
        return result;
    }

    // ---------- POST /api/lookups/{type} ----------
    @PostMapping("/{type}")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> addLookup(@PathVariable String type,
                                         @RequestBody Map<String, Object> data) {
        LookupTable table = newInstance(type);
        table.populate(data);
        table.create();
        return toResponse(table);
    }

    // ---------- PUT /api/lookups/{type}/{id} ----------
    @PutMapping("/{type}/{id}")
    public Map<String, Object> updateLookup(@PathVariable String type,
                                            @PathVariable String id,
                                            @RequestBody Map<String, Object> data) {
        LookupTable table = findOrFail(type, id);
        table.populate(data);
        table.edit();
        return toResponse(table);
    }

    // ---------- DELETE /api/lookups/{type}/{id} ----------
    @DeleteMapping("/{type}/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLookup(@PathVariable String type, @PathVariable String id) {
        findOrFail(type, id).delete();
    }

    // ---------- helpers ----------

    /** Picks the right model class for the URL segment. */
    private static LookupTable newInstance(String type) {
        return switch (type) {
            case "skills" -> new Skill();
            case "item-categories" -> new ItemCategory();
            case "need-categories" -> new NeedCategory();
            default -> throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Unknown lookup type: " + type);
        };
    }

    /** Loads a record, or answers 404 if it does not exist. */
    private static LookupTable findOrFail(String type, String id) {
        LookupTable table = newInstance(type);
        if (table.read(id) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Record not found");
        }
        return table;
    }

    private static Map<String, Object> toResponse(LookupTable table) {
        Map<String, Object> response = new LinkedHashMap<>(table.toMap());
        response.put("id", table.getId());
        return response;
    }

    /** A malformed MongoDB id becomes a clean 400 instead of a 500. */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String badRequest(IllegalArgumentException e) {
        return "Invalid id or data: " + e.getMessage();
    }
}
