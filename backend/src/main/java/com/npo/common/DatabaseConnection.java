package com.npo.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

/**
 * Singleton pattern: one shared MongoDB client for the application.
 * Models use this class for their CRUD operations.
 */
public final class DatabaseConnection {

    // volatile ensures safe publication when using double-checked locking.
    private static volatile DatabaseConnection instance;

    private final MongoClient client;
    private final MongoDatabase database;

    private DatabaseConnection() {
        String uri = System.getenv().getOrDefault(
                "MONGODB_URI",
                "mongodb://localhost:27017/?serverSelectionTimeoutMS=3000"
        );

        client = MongoClients.create(uri);
        database = client.getDatabase("npo");
    }

    /**
     * Returns the single instance, creating it safely on first use.
     */
    public static DatabaseConnection getInstance() {
        if (instance == null) {
            synchronized (DatabaseConnection.class) {
                if (instance == null) {
                    instance = new DatabaseConnection();
                }
            }
        }
        return instance;
    }

    public MongoCollection<Document> getCollection(String name) {
        return database.getCollection(name);
    }

    /** Inserts a document and returns its generated id. */
    public String insert(String collection, Map<String, Object> data) {
        Document doc = new Document(data);
        getCollection(collection).insertOne(doc);
        return doc.getObjectId("_id").toHexString();
    }

    /** Returns all documents matching the filter. */
    public List<Document> select(String collection, Bson filter) {
        return getCollection(collection)
                .find(filter)
                .into(new ArrayList<>());
    }

    public Document selectById(String collection, String id) {
        return getCollection(collection).find(byId(id)).first();
    }

    public boolean update(
            String collection,
            String id,
            Map<String, Object> data
    ) {
        return getCollection(collection)
                .updateOne(
                        byId(id),
                        new Document("$set", new Document(data))
                )
                .getMatchedCount() > 0;
    }

    public boolean delete(String collection, String id) {
        return getCollection(collection)
                .deleteOne(byId(id))
                .getDeletedCount() > 0;
    }

    /** Returns true if MongoDB answers a ping. */
    public boolean ping() {
        try {
            database.runCommand(new Document("ping", 1));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static Bson byId(String id) {
        return Filters.eq("_id", new ObjectId(id));
    }

    /** Releases the MongoDB client on application shutdown. */
    public void close() {
        client.close();
    }
}
