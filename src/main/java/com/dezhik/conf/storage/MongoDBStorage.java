package com.dezhik.conf.storage;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.*;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MongoDBStorage implements Storage {
    private static final Logger log = LoggerFactory.getLogger(MongoDBStorage.class);
    private static final String COLUMN_MODULE = "module";
    private static final String COLUMN_HOST = "host";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_VALUE = "val";
    private static final String COLUMN_VERSION = "ver";
    private static final String COLUMN_MODULES = "ms";
    private static final String COLUMN_DELETED = "del";
    private static final String COLUMN_CREATE_DATE = "cd";
    private static final String COLUMN_UPDATE_DATE = "ud";
    private static final String COLUMN_LAST_READ_DATE = "rd";

    private static final int DUPLICATE_WRITE_EXCEPTION = 11000;

    private static final Document SEQ_INCREMENT = new Document("$inc", new Document(COLUMN_VALUE, 1L))
            .append("$set", new Document(COLUMN_UPDATE_DATE, new Date()));

    private static final FindOneAndUpdateOptions UPSERT = new FindOneAndUpdateOptions()
            .returnDocument(ReturnDocument.AFTER)
            .upsert(true);

    private final String collectionName;
    private final MongoCollection<Document> collection;
    private final MongoCollection<Document> moduleCollection;
    private final MongoCollection<Document> seqCollection;
    private final MongoCollection<Document> clientCollection;


    public MongoDBStorage() {
        String dbUrl = System.getProperty("mongodb.url") != null ? System.getProperty("mongodb.url") : "mongodb://127.0.0.1:27017";
        String dbName = System.getProperty("mongodb.name") != null ? System.getProperty("mongodb.name") : "conf";
        collectionName = System.getProperty("mongodb.collection.name") != null ? System.getProperty("mongodb.collection.name") : "conf";

        MongoClientSettings.Builder settingsBuilder = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(dbUrl));
        String dbUser = System.getProperty("mongodb.user");
        String dbPassword = System.getProperty("mongodb.password");
        if (dbUser != null && dbPassword != null) {
            settingsBuilder.credential(MongoCredential.createCredential(dbUser, dbName, dbPassword.toCharArray()));
        }

        log.info("MongoDB initialization started.");
        final MongoClient mongoClient = MongoClients.create(settingsBuilder.build());

        collection = mongoClient
                .getDatabase(dbName)
                .getCollection(collectionName);
        seqCollection = mongoClient
                .getDatabase(dbName)
                .getCollection("sequence");
        moduleCollection = mongoClient
                .getDatabase(dbName)
                .getCollection("modules");
        clientCollection = mongoClient
                .getDatabase(dbName)
                .getCollection("clients");

        collection.createIndex(
                Indexes.ascending(COLUMN_MODULE, COLUMN_HOST, COLUMN_NAME),
                new IndexOptions().unique(true).sparse(true)
        );

        log.info("MongoDB initialization completed.");
    }

    private Bson constructFindQueryByFilter(PropertyFilter filter) {
        final List<Bson> conditions = new ArrayList<>();
        if (filter.getModule() != null) {
            conditions.add(Filters.eq(COLUMN_MODULE, filter.getModule()));
        }
        if (filter.getPropertyNames() != null && filter.getPropertyNames().size() > 0) {
            conditions.add(Filters.in(COLUMN_NAME, filter.getPropertyNames()));
        } else if (filter.getSearchQueryName() != null) {
            conditions.add(Filters.regex(COLUMN_NAME, filter.getSearchQueryName()));
        }

        if (filter.getVersion() != null) {
            // api request for diff, all recently occurred deletions should be returned to client
            conditions.add(Filters.gt(COLUMN_VERSION, filter.getVersion()));
        }


        if (conditions.size() == 1) {
            return conditions.get(0);
        } else if (conditions.size() > 1) {
            return Filters.and(conditions.toArray(new Bson[conditions.size()]));
        }
        return null;
    }

    /**
     * @param filter
     * @return all properties corresponding to the filter parameters.
     * Also returns deleted properties iff filter.isClientDiffRequest() is set to true.
     */
    @Override
    public List<Property> getByFilter(PropertyFilter filter) {
        final Bson query = constructFindQueryByFilter(filter);

        final MongoCursor<Document> cursor = query != null
                ? collection.find(query).iterator()
                : collection.find().iterator();

        final List<Property> results = new ArrayList<>();

        while (cursor.hasNext()) {
            Document doc = cursor.next();
            if (!filter.isClientDiffRequest() && Boolean.TRUE.equals(doc.getBoolean(COLUMN_DELETED))) {
                continue;
            }
            if (!filter.getHosts().isEmpty() && !filter.getHosts().contains(doc.getString(COLUMN_HOST))) {
                continue;
            }

            results.add(fromDocument(doc));
        }

        cursor.close();

        return results;
    }

    @Override
    public boolean create(final String module, final String host, final String name, final String value) {
        final MongoCursor<Document> cursor = collection.find(Filters.and(
                Filters.eq(COLUMN_MODULE, module),
                Filters.eq(COLUMN_HOST, host),
                Filters.eq(COLUMN_NAME, name)
        )).iterator();

        final Document doc = cursor.hasNext() ? cursor.next() : null;
        final boolean exists = doc != null;

        if (exists && !Boolean.TRUE.equals(doc.getBoolean(COLUMN_DELETED))) {
            // property already exists & not in deleted state
            // made for avoiding unnecessary sequence modification
            return false;
        }

        final Date now = new Date();
        // returns MongoWriteException with code 11000 duplicate key error
        final Document newDoc = new Document()
                .append(COLUMN_MODULE, module)
                .append(COLUMN_HOST, host)
                .append(COLUMN_NAME, name)
                .append(COLUMN_VALUE, value)
                .append(COLUMN_CREATE_DATE, now)
                .append(COLUMN_UPDATE_DATE, now)
                .append(COLUMN_DELETED, null)
                .append(COLUMN_VERSION, nextVersion(module));

        try {
            if (!exists) {
                collection.insertOne(newDoc);
            } else {
                // exists with deleted column set to true
                Document prev = collection.findOneAndUpdate(
                        new Document()
                                .append(COLUMN_MODULE, module)
                                .append(COLUMN_HOST, host)
                                .append(COLUMN_NAME, name)
                                .append(COLUMN_DELETED, true),
                        new Document("$set", newDoc)
                );
                return prev != null;
            }
        } catch (MongoWriteException me) {
            if (me.getCode() == 11000) {
                // todo
                System.out.println("Already exists exception");
                return false; // no-op if duplicate
            }
            throw me;
        }
        return true;
    }

    @Override
    public boolean update(String module, String host, String name, String value, long lastRevision) {
        final Bson filter = Filters.and(
                Filters.eq(COLUMN_MODULE, module),
                Filters.eq(COLUMN_HOST, host),
                Filters.eq(COLUMN_NAME, name),
                Filters.eq(COLUMN_VERSION, lastRevision)
        );

        if (!collection.find(filter).iterator().hasNext()) {
            // no property matched
            // made for avoiding unnecessary sequence modification
            return false;
        }

        UpdateResult result = collection.updateOne(
                filter,
                Updates.combine(
                        Updates.set(COLUMN_VALUE, value),
                        Updates.set(COLUMN_UPDATE_DATE, new Date()),
                        Updates.set(COLUMN_VERSION, nextVersion(module))
                )
        );
        return result.wasAcknowledged() && result.isModifiedCountAvailable() && result.getModifiedCount() > 0;
    }

    @Override
    public boolean delete(String module, String host, String name, long lastRevision) {
        // todo schedule cleanup for deleted properties

        final Bson filter = Filters.and(
                Filters.eq(COLUMN_MODULE, module),
                Filters.eq(COLUMN_HOST, host),
                Filters.eq(COLUMN_NAME, name),
                Filters.eq(COLUMN_VERSION, lastRevision)
        );

        if (!collection.find(filter).iterator().hasNext()) {
            // no property matched
            // made for avoiding unnecessary sequence modification
            return false;
        }

        UpdateResult result = collection.updateOne(
                filter,
                Updates.combine(
                        Updates.set(COLUMN_DELETED, true),
                        Updates.set(COLUMN_UPDATE_DATE, new Date()),
                        Updates.set(COLUMN_VERSION, nextVersion(module))
                )
        );
        return result.wasAcknowledged() && result.isModifiedCountAvailable() && result.getModifiedCount() > 0;
    }

    @Override
    public boolean createModule(String module) {
        try {
            seqCollection.insertOne(
                    new Document("_id", module)
                            .append(COLUMN_VALUE, 0l)
                            .append(COLUMN_UPDATE_DATE, new Date())
            );
        } catch (MongoWriteException mwe) {
            if (mwe.getCode() != DUPLICATE_WRITE_EXCEPTION)
                throw mwe;

            return false;
        }

        return true;
    }

    @Override
    public List<PropertyModule> getModules() {
        MongoCursor<Document> it = seqCollection.find().iterator();
        final List<PropertyModule> result = new ArrayList<>();
        while (it.hasNext()) {
            Document doc = it.next();
            PropertyModule module = new PropertyModule();
            module.setName(doc.getString("_id"));
            module.setLastUpdate(doc.getDate(COLUMN_UPDATE_DATE));
            module.setLastRead(doc.getDate(COLUMN_LAST_READ_DATE));
            result.add(module);
        }
        // todo sort
        return result;
    }

    @Override
    public void registerClient(String host, long lastVersion, String modulesList) {
        clientCollection.findOneAndUpdate(
            Filters.eq("_id", host),
            Updates.combine(
                Updates.set(COLUMN_LAST_READ_DATE, new Date()),
                Updates.set(COLUMN_VERSION, lastVersion),
                Updates.set(COLUMN_MODULES, modulesList)
            ),
            UPSERT
        );
    }

    @Override
    public List<ClientModel> getClients() {
        MongoCursor<Document> it = clientCollection.find().iterator();
        List<ClientModel> result = new ArrayList<>();
        while (it.hasNext()) {
            Document doc = it.next();
            ClientModel client = new ClientModel();
            client.setHost(doc.getString("_id"));
            client.setLastRead(doc.getDate(COLUMN_LAST_READ_DATE));
            client.setVersion(doc.getLong(COLUMN_VERSION));
            client.setModulesList(doc.getString(COLUMN_MODULES));
            result.add(client);
        }
        return result;
    }

    // todo un-synchronized sequence modification and property modification
    // lock is needed for consistency and to prevent race: when ver.18 is visible while ver.17 modification is still running
    // in this case client may never receive 17th version until the whole client cache is invalidated
    private long nextVersion(String module) {
        Document res = seqCollection.findOneAndUpdate(new Document("_id", module), SEQ_INCREMENT, UPSERT);
        return res.getLong(COLUMN_VALUE);
    }

    @Override
    public Date getLatestUpdate(String module) {
        MongoCursor<Document> doc = seqCollection.find(Filters.eq("_id", module)).iterator();
        return doc.hasNext() ? doc.next().getDate(COLUMN_UPDATE_DATE) : null;
    }

    @Override
    public long getCurrentVersion(String module) {
        MongoCursor<Document> doc = seqCollection.find(Filters.eq("_id", module)).iterator();
        return doc.hasNext() ? doc.next().getLong(COLUMN_VALUE) : -1;
    }

    private Property fromDocument(Document dbEntity) {
        return new Property(
                dbEntity.getString(COLUMN_MODULE),
                dbEntity.getString(COLUMN_NAME),
                dbEntity.getString(COLUMN_HOST),
                dbEntity.getBoolean(COLUMN_DELETED, false),
                dbEntity.getString(COLUMN_VALUE),
                dbEntity.getLong(COLUMN_VERSION)
        );
    }
}
