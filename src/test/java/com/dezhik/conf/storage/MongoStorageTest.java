package com.dezhik.conf.storage;

import com.dezhik.conf.server.ConfServer;
import com.mongodb.client.model.Collation;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfig;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class MongoStorageTest {

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

    private final String moduleFirst = "m1";
    private final String moduleSecond = "m2";
    private final Property createdDefault = new Property(moduleFirst, "p1", ConfServer.DEFAULT_HOST, false, "default.value", 1);
    private final Property createdHost = new Property(moduleFirst, "p1", "h1", false, "p1.value", 2);
    private final Property deletedHost = new Property(moduleFirst, "p1", "h1", true, "p1.value", 3);

    @Test
    public void moduleCreate() {
        final Storage storage = new MongoDBStorage();

        Assert.assertEquals(0, storage.getModules().size());
        Assert.assertEquals(-1, storage.getCurrentVersion(moduleFirst));

        // create
        Assert.assertTrue(storage.createModule(moduleFirst));

        final List<PropertyModule> modules = storage.getModules();
        Assert.assertNotNull(modules);
        Assert.assertEquals(1, modules.size());

        final PropertyModule module = modules.get(0);
        final Date now = new Date();
        Assert.assertEquals(moduleFirst, module.getName());
        Assert.assertTrue(module.getLastUpdate().before(now));
        Assert.assertNull(module.getLastRead());

        Assert.assertEquals(0, storage.getCurrentVersion(moduleFirst));
        Assert.assertEquals(module.getLastUpdate(), storage.getLatestUpdate(moduleFirst));

        // try to create the same module
        Assert.assertFalse(storage.createModule(moduleFirst));

        // check that duplicate creation has no side effects and the original module stayed intact
        final List<PropertyModule> modulesAfterUpdate = storage.getModules();
        Assert.assertNotNull(modulesAfterUpdate);
        Assert.assertEquals(1, modulesAfterUpdate.size());

        final PropertyModule moduleAfterUpdate = modulesAfterUpdate.get(0);
        Assert.assertEquals(moduleAfterUpdate, module);

        Assert.assertEquals(0, storage.getCurrentVersion(moduleFirst));
        Assert.assertEquals(moduleAfterUpdate.getLastUpdate(), storage.getLatestUpdate(moduleFirst));

    }

    @Test
    public void createAndDelete() {
        Storage storage = new MongoDBStorage();
        PropertyFilter.Builder allFilter = new PropertyFilter.Builder(createdDefault.getModule()).version(0L);

        Assert.assertEquals(0, storage.getByFilter(allFilter.build()).size());

        Assert.assertTrue(storage.create(createdDefault.getModule(), createdDefault.getHost(), createdDefault.getName(), createdDefault.getValue()));
        // query properties without hosts filter
        Assert.assertEquals(1, storage.getByFilter(allFilter.build()).size());

        List<String> hosts = Arrays.asList(createdHost.getHost(), createdDefault.getHost());

        // specify hosts
        allFilter
            .hosts(hosts)
            .propertyNames(Collections.singletonList(createdDefault.getName()));

        List<Property> dbValueByHosts = storage.getByFilter(allFilter.build());

        Assert.assertEquals(1, dbValueByHosts.size());
        Assert.assertTrue(dbValueByHosts.contains(createdDefault));

        // check arbitrary host query still returns null
        Property dbValueRand = storage.getExactProperty(createdDefault.getModule(),"randHost", createdDefault.getName());
        Assert.assertNull(dbValueRand);

        Property dbValueDefault = storage.getExactProperty(createdDefault.getModule(), createdDefault.getHost(), createdDefault.getName());
        Assert.assertEquals(createdDefault, dbValueDefault);

        Assert.assertTrue(storage.create(createdHost.getModule(), createdHost.getHost(), createdHost.getName(), createdHost.getValue()));

        List<Property> dbValues = storage.getByFilter(allFilter.build());

        System.out.println(allFilter.build() + "\nresult: " + dbValues.get(0));

        Assert.assertEquals(2, dbValues.size());
        Assert.assertTrue(dbValues.contains(createdDefault));
        Assert.assertTrue(dbValues.contains(createdHost));

        storage.delete(createdHost.getModule(), createdHost.getHost(), createdHost.getName(), createdHost.getVersion());
        Property deletedDb = storage.getExactProperty(createdHost.getModule(), createdHost.getHost(), createdHost.getName());
        Assert.assertNull(deletedDb);

        List<Property> valuesWithDeleted = storage.getByFilter(allFilter.build());

        Assert.assertEquals(2, valuesWithDeleted.size());
        Assert.assertTrue(valuesWithDeleted.contains(createdDefault));
        Assert.assertTrue(valuesWithDeleted.contains(deletedHost));
    }

}
