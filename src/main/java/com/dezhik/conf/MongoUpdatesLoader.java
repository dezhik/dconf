package com.dezhik.conf;

import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;

import org.bson.Document;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;

/**
 * @author ilya.dezhin
 */
public class MongoUpdatesLoader implements UpdatesLoader {

    private final MongoCollection<Document> collection;

    public MongoUpdatesLoader() {
        String dbUrl = System.getProperty("confDbUrl") != null ? System.getProperty("confDbUrl") : "mongodb://127.0.0.1:27017";
        String dbName = System.getProperty("confDbName") != null ? System.getProperty("confDbName") : "custos";
        MongoClient mongoClient = MongoClients.create(dbUrl);
        collection = mongoClient
                .getDatabase(dbName)
                .getCollection("conf");
    }

    public MongoUpdatesLoader(MongoCollection<Document> collection) {
        this.collection = collection;
    }

    @Override
    public @NotNull ConfValues getUpdates(final long lastUpdateTime) {
        final MongoCursor<Document> res = collection
                .find(Filters.gt("upt", lastUpdateTime))
                .iterator();

        final Map<String, String> newValues = new HashMap<>();
        long newLastUpdateTime = lastUpdateTime;
        while (res.hasNext()) {
            Document entity = res.next();
            newValues.put(entity.getString("_id"), entity.getString("v"));
            newLastUpdateTime = Math.max(newLastUpdateTime, entity.getLong("upt"));
        }

        return new ConfValues(newValues, lastUpdateTime);
    }
}
