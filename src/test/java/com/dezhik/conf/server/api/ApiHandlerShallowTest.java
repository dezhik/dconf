package com.dezhik.conf.server.api;

import com.dezhik.conf.loader.UpdatesResponse;
import com.dezhik.conf.storage.MongoDBStorage;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfig;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import org.junit.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class ApiHandlerShallowTest {

    private static MongodProcess mongod;

    @BeforeClass
    public static void start() throws IOException {
        MongodStarter starter = MongodStarter.getDefaultInstance();

        System.setProperty("mongodb.url", "mongodb://127.0.0.1:27019");

        MongodConfig mongodConfig = MongodConfig.builder()
                .version(Version.Main.PRODUCTION)
                .net(new Net(27019, false))
                .build();

        MongodExecutable mongodExecutable = starter.prepare(mongodConfig);
        mongod = mongodExecutable.start();
    }

    @AfterClass
    public static void stop() {
        if (mongod != null) {
            mongod.stop();
        }
    }

    private void checkError(String input) throws IOException {
        ApiHandler handler = new ApiHandler(new MongoDBStorage());
        UpdatesResponse resp = handler.handleImpl(new ByteArrayInputStream(input.getBytes()));
        Assert.assertNotNull(resp);
        Assert.assertNotNull(resp.error);
        Assert.assertNull(resp.modules);
    }

    @Test
    public void invalidJson() throws IOException {
        checkError("");
    }

    @Test
    public void emptyJson() throws IOException {
        checkError("{}");
    }

    @Test
    public void emptyHost() throws IOException {
        checkError("{\"host\":\"\"}");
    }

    @Test
    public void emptyModules() throws IOException {
        checkError("{\"host\":\"h1\", \"modules\":[]}");
    }

    @Test
    public void emptyVersion() throws IOException {
        checkError("{\"host\":\"h1\", \"modules\":[\"m1\"]}");
    }

    @Test
    public void invalidModuleNameFormat() throws IOException {
        checkError("{\"host\":\"h1\", \"modules\":[111], \"version\":0}");
    }

    @Test
    public void emptyModuleVersion() throws IOException {
        checkError("{\"host\":\"h1\", \"modules\":[{\"name\": \"m1\"}]}");
    }

    @Test
    public void invalidModuleVersionFormat() throws IOException {
        checkError("{\"host\":\"h1\", \"modules\":[\"m1\"], \"version\":\"\"}");
    }

    @Test
    public void moduleRequest() throws IOException {
        ApiHandler handler = new ApiHandler(new MongoDBStorage());
        UpdatesResponse resp = handler.handleImpl(new ByteArrayInputStream(
                "{\"host\":\"h1\", \"modules\":[\"m1\"], \"version\":1}".getBytes())
        );
        Assert.assertNotNull(resp);
        Assert.assertNull(resp.error);
        Assert.assertNotNull(resp.modules);
        Assert.assertEquals(1, resp.modules.size());
        UpdatesResponse.Module firstModule = resp.modules.get(0);
        Assert.assertEquals("m1", firstModule.name);
        Assert.assertTrue(firstModule.properties.isEmpty());
    }

    @Test
    public void twoModulesRequest() throws IOException {
        ApiHandler handler = new ApiHandler(new MongoDBStorage());
        UpdatesResponse resp = handler.handleImpl(new ByteArrayInputStream(
                "{\"host\":\"h1\", \"modules\":[\"m1\",\"m2\"], \"version\":1}".getBytes())
        );
        Assert.assertNotNull(resp);
        Assert.assertNull(resp.error);
        Assert.assertNotNull(resp.modules);
        Assert.assertEquals(2, resp.modules.size());

        UpdatesResponse.Module firstModule = resp.modules.get(0);
        Assert.assertEquals("m1", firstModule.name);
        Assert.assertTrue(firstModule.properties.isEmpty());

        UpdatesResponse.Module secondModule = resp.modules.get(1);
        Assert.assertEquals("m2", secondModule.name);
        Assert.assertTrue(secondModule.properties.isEmpty());
    }
}
