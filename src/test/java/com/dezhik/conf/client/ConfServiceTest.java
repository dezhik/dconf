package com.dezhik.conf.client;

import com.dezhik.conf.converter.StringConverter;
import com.dezhik.conf.loader.RemoteUpdatesLoader;
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

import java.io.IOException;

public class ConfServiceTest {
    private static final int SYNC_DELAY_SEC = 1;
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
        server = new ConfServer("127.0.0.1", 8080, storage);
        server.start();

    }

    @After
    public void stop() throws Exception {
        if (mongod != null) {
            mongod.stop();
        }
        server.stop();
    }

    private void waitForConfSync() throws InterruptedException {
        Thread.sleep(SYNC_DELAY_SEC * 1_500);
    }

    @Test
    public void duplicatedProperties() throws IOException, InterruptedException {
        final ConfService confService = new ConfService(
                new RemoteUpdatesLoader(DEFAULT_ENDPOINT, "h1"), SYNC_DELAY_SEC
        );

        try {
            confService.start();
            PropertyValue<String> originalProperty = confService.get("m1", "p1", StringConverter.INSTANCE, "defaultValue");
            Assert.assertEquals("defaultValue", originalProperty.getValue());

            PropertyValue<String> duplicateProperty = confService.get("m1", "p1", StringConverter.INSTANCE, "defaultValue2");
            Assert.assertEquals("defaultValue", originalProperty.getValue());
            Assert.assertEquals("defaultValue2", duplicateProperty.getValue());

            // both should be updated on remote change
            storage.create("m1", ConfServer.DEFAULT_HOST, "p1", "newValue");
            waitForConfSync(); // remote sync
            Assert.assertEquals("newValue", originalProperty.getValue());
            Assert.assertEquals("newValue", duplicateProperty.getValue());

            // both should be returned to default after remote deletion
            storage.delete("m1", ConfServer.DEFAULT_HOST, "p1", 1L);
            waitForConfSync();
            Assert.assertEquals("defaultValue", originalProperty.getValue());
            Assert.assertEquals("defaultValue2", duplicateProperty.getValue());
        } finally {
            confService.stop();
        }
    }

    public void startWithNoModuleAdded() throws IOException {
        ConfService service = new ConfService(
                new RemoteUpdatesLoader("http://localhost:31213/api/", "h1"), SYNC_DELAY_SEC
        );
        service.start(); // if no module added - than no exception is raised
    }

    @Test
    public void loadOnStartAndDelete() throws Exception {
        final ConfService confService = new ConfService(new RemoteUpdatesLoader(DEFAULT_ENDPOINT, "h1"), SYNC_DELAY_SEC);

        try {
            // get(..) invokes ConfService lazy init
            PropertyValue<String> property = confService.get("m1", "p1", StringConverter.INSTANCE, "defaultValue");
            Assert.assertEquals("defaultValue", property.getValue());

            storage.create("m1", ConfServer.DEFAULT_HOST, "p1", "newDefaultValue");
            waitForConfSync(); // remote sync

            Assert.assertEquals("newDefaultValue", property.getValue());

            storage.create("m1", "h1", "p1", "newValue");
            waitForConfSync(); // remote sync
            Assert.assertEquals("newValue", property.getValue());

            storage.delete("m1", "h1", "p1", 2L);
            waitForConfSync();
            Assert.assertEquals("newDefaultValue", property.getValue());

            storage.delete("m1", ConfServer.DEFAULT_HOST, "p1", 1L);
            waitForConfSync();
            Assert.assertEquals("defaultValue", property.getValue());
        } finally {
            confService.stop();
        }
    }

//    @Test
//    public void deletion() throws Exception {
//        final ConfService confService = new ConfService(new RemoteUpdatesLoader(DEFAULT_ENDPOINT, "h1"), SYNC_DELAY_SEC);
//
//        try {
//            PropertyValue<String> property = confService.get("m1", "p1", StringConverter.INSTANCE, "defaultValue");
//
//            storage.create("m1", "h1", "p1", "newValue");
//            confService.start();
//
//            Assert.assertEquals("newValue", property.getValue());
//
//            waitForConfSync();
//
//        } finally {
//            confService.stop();
//        }
//    }

    @Test
    public void postponedPropertyAssociation() throws Exception {
        final ConfService confService = new ConfService(new RemoteUpdatesLoader(DEFAULT_ENDPOINT, "h1"), SYNC_DELAY_SEC);

        try {
            storage.create("m1", "h1", "p1", "newValue");
            confService.start();

            PropertyValue<String> property = confService.get("m1", "p1", StringConverter.INSTANCE, "defaultValue");
            Assert.assertEquals("newValue", property.getValue());
        } finally {
            confService.stop();
        }
    }

}
