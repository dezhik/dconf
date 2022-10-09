package com.dezhik.conf.loader;

import com.dezhik.conf.client.ConfValues;
import com.dezhik.conf.server.ConfServer;
import com.dezhik.conf.storage.MongoDBStorage;
import com.dezhik.conf.storage.Storage;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfig;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

public class RemoteUpdatesLoaderTest {
    private static final String DEFAULT_ENDPOINT = "http://localhost:8080/api/";

    private MongodProcess mongod;
    private Storage storage;
    private ConfServer server;

    @Before
    public void start() throws Exception {
        MongodStarter starter = MongodStarter.getDefaultInstance();

        System.setProperty("mongodb.url", "mongodb://127.0.0.1:27019");

        MongodConfig mongodConfig = MongodConfig.builder()
                .version(Version.Main.PRODUCTION)
                .net(new Net(27019, false))
                .build();

        MongodExecutable mongodExecutable = starter.prepare(mongodConfig);
        mongod = mongodExecutable.start();

        storage = new MongoDBStorage();
        server = new ConfServer("localhost", 8080, storage);
        server.start();
    }

    @After
    public void stop() throws Exception {
        if (mongod != null) {
            mongod.stop();
        }
        server.stop();
    }

    @Test
    public void emptyCheck() throws Exception {
        RemoteUpdatesLoader loader = new RemoteUpdatesLoader(DEFAULT_ENDPOINT, "h1");
        Map<String, ConfValues> updates = loader.getUpdates(Collections.singletonList("m1"),0);
        Assert.assertEquals(1, updates.size());
        Assert.assertTrue(updates.containsKey("m1"));
        ConfValues moduleValues = updates.get("m1");
        Assert.assertEquals(0, moduleValues.values.size());
    }

    @Test
    public void checkWithVersionIncrement() throws Exception {
        storage.create("m1", "h1", "p1", "v1");

        RemoteUpdatesLoader loader = new RemoteUpdatesLoader(DEFAULT_ENDPOINT, "h1");
        Map<String, ConfValues> updates = loader.getUpdates(Collections.singletonList("m1"), 0);
        Assert.assertEquals(1, updates.size());
        Assert.assertTrue(updates.containsKey("m1"));

        ConfValues moduleValues = updates.get("m1");

        Assert.assertEquals(1, moduleValues.values.size());
        Assert.assertEquals("v1", moduleValues.values.get("p1"));

        // request with returned version
        updates = loader.getUpdates(Collections.singletonList("m1"), moduleValues.version);
        Assert.assertEquals(1, updates.size());

        moduleValues = updates.get("m1");
        Assert.assertEquals(0, moduleValues.values.size());
    }
}
