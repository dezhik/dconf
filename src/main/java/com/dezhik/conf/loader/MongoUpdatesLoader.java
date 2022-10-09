package com.dezhik.conf.loader;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import com.dezhik.conf.client.ConfValues;
import org.bson.Document;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;

public class MongoUpdatesLoader implements UpdatesLoader {

    private final MongoCollection<Document> collection;

    public MongoUpdatesLoader() {
        String dbUrl = System.getProperty("mongodb.url") != null ? System.getProperty("mongodb.url") : "mongodb://127.0.0.1:27017";
        String dbName = System.getProperty("mongodb.url") != null ? System.getProperty("mongodb.url") : "custos";
        MongoClient mongoClient = MongoClients.create(dbUrl);
        collection = mongoClient
                .getDatabase(dbName)
                .getCollection("conf");
    }

    public MongoUpdatesLoader(MongoCollection<Document> collection) {
        this.collection = collection;
    }

    @Override
    public Map<String, ConfValues> getUpdates(List<String> modules, final long lastUpdateTime) {
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

        return Collections.singletonMap("mongoLoader", new ConfValues(newValues, newLastUpdateTime));
    }
}
