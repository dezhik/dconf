package com.dezhik.conf.server.api;

import com.dezhik.conf.loader.UpdatesResponse;
import com.dezhik.conf.server.ConfServer;
import com.dezhik.conf.storage.ClientModel;
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class ApiHandlerTest {

    private MongodProcess mongod;

    @Before
    public void start() throws IOException {
        MongodStarter starter = MongodStarter.getDefaultInstance();

        System.setProperty("mongodb.url", "mongodb://127.0.0.1:27019");

        MongodConfig mongodConfig = MongodConfig.builder()
                .version(Version.Main.PRODUCTION)
                .net(new Net(27019, false))
                .build();

        MongodExecutable mongodExecutable = starter.prepare(mongodConfig);
        mongod = mongodExecutable.start();
    }

    @After
    public void stop() {
        if (mongod != null) {
            mongod.stop();
        }
    }

    private void processAndCheck(ApiHandler handler,
                                 String json,
                                 int modules,
                                 String name,
                                 long version,
                                 List<UpdatesResponse.Module.Entry> entries) throws IOException
    {
        UpdatesResponse resp = handler.handleImpl(new ByteArrayInputStream(
                json.getBytes()
        ));
        Assert.assertNull(resp.error);

        Assert.assertEquals(modules, resp.modules.size());
        UpdatesResponse.Module module = resp.modules.get(0);
        Assert.assertEquals(name, module.name);
        Assert.assertEquals(version, module.lastVersion);
        Assert.assertEquals(entries, module.properties);
    }

    @Test
    public void checkDefaultAndOverrideRequest() throws IOException {
        Storage storage = new MongoDBStorage();
        ApiHandler handler = new ApiHandler(storage);
        processAndCheck(
                handler,
                "{\"host\":\"h1\", \"modules\":[\"m1\"], \"version\":0}",
                1,
                "m1",
                0L,
                Collections.emptyList()
        );

        storage.create("m1", ConfServer.DEFAULT_HOST, "p1", "v1");
        processAndCheck(
                handler,
                "{\"host\":\"h1\", \"modules\":[\"m1\"], \"version\":0}",
                1,
                "m1",
                1L,
                Collections.singletonList(new UpdatesResponse.Module.Entry("p1", "v1", 1L))
        );

        // host value overrides DEFAULT_HOST
        storage.create("m1", "h1", "p1", "v2");
        processAndCheck(
                handler,
                "{\"host\":\"h1\", \"modules\":[\"m1\"], \"version\":0}",
                1,
                "m1",
                2L,
                Collections.singletonList(new UpdatesResponse.Module.Entry("p1", "v2", 2L))
        );

        // delete host value, DEFAULT_HOST value should be returned
        storage.delete("m1", "h1", "p1", 2L);
        processAndCheck(
                handler,
                "{\"host\":\"h1\", \"modules\":[\"m1\"], \"version\":0}",
                1,
                "m1",
                3L, // deletion increases module's version
                Collections.singletonList(new UpdatesResponse.Module.Entry("p1", "v1", 1L))
        );

        // delete DEFAULT_HOST value
        // checking that empty properties list is return when calling as 'new' host with 0 version
        storage.delete("m1", ConfServer.DEFAULT_HOST, "p1", 1);
        processAndCheck(
                handler,
                "{\"host\":\"h1\", \"modules\":[\"m1\"], \"version\":0}",
                1,
                "m1",
                4L, // deletion increases module's version
                Collections.emptyList()
        );


        // checking that request with version > 0 returns deletion
        processAndCheck(
                handler,
                "{\"host\":\"h1\", \"modules\":[\"m1\"], \"version\":2}",
                1,
                "m1",
                4L,
                Collections.singletonList(new UpdatesResponse.Module.Entry("p1", null, 3L))
        );

    }

    @Test
    public void clientRegistyCheck() throws IOException {
        Storage storage = new MongoDBStorage();
        ApiHandler handler = new ApiHandler(storage);

        Assert.assertTrue(storage.getClients().isEmpty());

        processAndCheck(
                handler,
                "{\"host\":\"h1\", \"modules\":[\"m1\"], \"version\":0}",
                1,
                "m1",
                0L,
                Collections.emptyList()
        );

        Assert.assertEquals(1, storage.getClients().size());
        ClientModel client = storage.getClients().get(0);
        Assert.assertEquals("h1", client.getHost());
        final long timeDiff = System.currentTimeMillis() - client.getLastRead().getTime();
        Assert.assertTrue(timeDiff >= 0 && timeDiff <= 1_000);
        Assert.assertEquals(0, client.getVersion());
    }
}
