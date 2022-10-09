package com.dezhik.conf.client;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.dezhik.conf.converter.Converter;
import com.dezhik.conf.loader.FSUpdatesLoader;
import com.dezhik.conf.loader.UpdatesLoader;
import org.apache.http.conn.HttpHostConnectException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sun.misc.Unsafe;

/**
 * @author ilya.dezhin
 */
public class ConfService {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    public static final String VERSION = "0.1";

    private static final String DELIMITER = ":";
    private volatile Map<String, ConfValues> values = new ConcurrentHashMap<>();
    private volatile long version = -1;
    private final List<String> modules = new ArrayList<>();

    private final Lock lock = new ReentrantLock();
    private volatile boolean started = false;

    /*
     * Used by UpdatesLoader to detect existing property and set new value for it when property update is loaded.
     * Prone to pollution with deleted properties.
     */
    private Map<String, List<PropertyValue<?>>> propertiesMap = new ConcurrentHashMap<>();
    private final Unsafe unsafe;
    private final long propertyValueOffset;
    private static volatile Thread updater;
    private final long reloadDelayMs;
    private final boolean startupSyncRequired;

    private final UpdatesLoader mainLoader;

    public ConfService(UpdatesLoader loader) {
        this(loader, 10);
    }

    public ConfService(UpdatesLoader loader, int reloadDelaySec) {
        this.mainLoader = loader;
        this.reloadDelayMs = TimeUnit.SECONDS.toMillis(reloadDelaySec);

        this.startupSyncRequired = "false".equalsIgnoreCase(System.getProperty("conf.startup.sync.required"));

        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            unsafe = (Unsafe) f.get(null);
            propertyValueOffset = unsafe.objectFieldOffset(PropertyValue.class.getDeclaredField("value"));
        } catch (NoSuchFieldException|IllegalAccessException e) {
            throw new IllegalStateException("can't get offset for value", e);
        }

    }

    /**
     * Proceeds initial config synchronization and starts daemon thread for pulling config updates.
     */
    @PostConstruct
    public void start() throws IOException {
        try {
            lock.lock();
            if (started) {
                log.error("ConfService is already initialized.");
                return;
            }

            // retry helps endure short term conf-server outages
            int retry = startupSyncRequired ? 3 : 1;
            while (retry-- > 0) {
                try {
                    initialSync();
                    started = true;
                    break;
                } catch (IOException io) {
                    if (retry == 0 && startupSyncRequired) {
                        throw new IllegalStateException("Startup failed, check conf-server availability " +
                                "or disable conf.startup.sync.required property", io);
                    }
                }

                log.warn("Retrying initialSync after failure.");
                try {
                    Thread.sleep(3_000);
                } catch (InterruptedException ignored) {}
            }

            // if sync failed but is not required the updater should eventually fetch most recent conf
            startUpdater();
        } finally {
            lock.unlock();
        }
    }

    private void initialSync() throws IOException {
        log.info("ConfService initial loading started.");
        // initial loading should be synced with service start-up
        // local files goes first and are loaded only once at startup, w/o dynamic runtime reloading
        final UpdatesLoader classpathLoader = new FSUpdatesLoader("classpath://conf/app.properties");
        final int cpCount = loadAndProcess(classpathLoader, false);

        // mb delete
        final UpdatesLoader dynamicLoader = new FSUpdatesLoader("/tmp/conf");
        final long now = System.currentTimeMillis();
        final int fsCount = loadAndProcess(dynamicLoader, false);

        final int totalCount = cpCount + fsCount + loadAndProcess(mainLoader, true);
        log.info("ConfService initial loading completed in {} ms, found in classpath {}, in fs {}, total {} properties. version {}",
                (System.currentTimeMillis() - now), cpCount, fsCount, totalCount, version);
    }

    private void startUpdater() {
        updater = new Thread(() -> {
            log.info("ConfService updater has been started.");

            try {
                Thread.sleep(reloadDelayMs);
            } catch (InterruptedException ignored) {}

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    loadAndProcess(mainLoader, true);
                } catch (HttpHostConnectException ce) {
                    log.warn("Conf-updater error: " + ce.getLocalizedMessage());
                } catch (Throwable e) {
                    log.warn("Conf-updater error: ", e);
                }
                try {
                    Thread.sleep(reloadDelayMs);
                } catch (InterruptedException ignored) {}
            }

            log.info("ConfService updater is terminated.");
        }, "Conf-updater");
        updater.setDaemon(true);
        updater.start();
    }

    @PreDestroy
    void stop() throws InterruptedException {
        if (!started)
            return;

        try {
            lock.lock();
            updater.interrupt();
            updater.join(reloadDelayMs);
            updater = null;
        } finally {
            lock.unlock();
        }
    }

    private int loadAndProcess(UpdatesLoader loader, boolean notify) throws IOException {
        return loadAndProcess(loader, modules, version, notify);
    }
    /**
     * Acquires update lock to guarantee that association of new properties
     * and updating of existing properties can't happen in parallel.
     *
     * @param loader
     * @param notify flag
     * @return
     */
    private int loadAndProcess(UpdatesLoader loader, List<String> modules, long version, boolean notify) throws IOException {
        try {
            lock.lock();

            if (modules.size() == 0) {
                log.warn("Skipping loadAndProcess phase: empty modules list");
                return 0;
            }

            final Map<String, ConfValues> updates = loader.getUpdates(modules, version);
            if (updates.size() == 0) {
                return 0;
            }

            int total = 0;
            for (Map.Entry<String, ConfValues> moduleUpdate : updates.entrySet()) {
                final String moduleName = moduleUpdate.getKey();
                final ConfValues newValues = moduleUpdate.getValue();
                final int processedCount = processModule(moduleName, newValues, notify);

                log.info("Loaded {} properties for module {}", processedCount, moduleName);

                final ConfValues moduleValues = values.get(moduleName);
                values.put(
                        moduleName,
                        moduleValues == null ? newValues : moduleValues.mergeWithUpdate(newValues)
                );

                // dirty hack and should be removed later
                this.version = Math.max(version, newValues.version);
                total += processedCount;
            }

            return total;
        } finally {
            lock.unlock();
        }
    }

    private int processModule(String module, ConfValues updates, boolean notify) {
        int count = 0;

        for (String deletedProperty : updates.getDeletions()) {
            count++;
            List<PropertyValue<?>> properties = propertiesMap.get(module + DELIMITER + deletedProperty);
            if (properties == null) {
                if (started) {
                    log.info("No associated property for {} / {}, deletion processing skipped.", module, deletedProperty);
                }
                continue;
            }

            properties.forEach(p -> {
                p.restoreDefault();
                log.info(String.format("%s / %s restored default", module, deletedProperty));
            });
        }

        for (Map.Entry<String, String> entry : updates.values.entrySet()) {
            count++;
            try {
                List<PropertyValue<?>> properties = propertiesMap.get(module + DELIMITER + entry.getKey());
                if (properties == null) {
                    if (started) {
                        log.info("No associated property for {} / {} : {}.", module, entry.getKey(), entry.getValue());
                    }
                    continue;
                }

                for (PropertyValue<?> property : properties) {
                    property.setValue(entry.getValue());

                    if (notify) {
                        List<Callable<?>> onChangeSubscribers = property.getOnChangeListeners();
                        if (onChangeSubscribers != null) {
                            onChangeSubscribers.forEach(callable -> {
                                try {
                                    callable.call();
                                } catch (Exception e) {
                                    log.error("error while notifying " + module + " / " + entry.getKey() + " onChange subscriber", e);
                                }
                            });
                        }
                    }

                    log.info("New value for {} / {} : {}. Property => {}. last update {}",
                            module, entry.getKey(), entry.getValue(), property.getValue(), updates.version);
                }
            } catch (Throwable th) {
                log.error("Can't convert property " + module + " / " + entry.getKey() + " with value " + entry.getValue(), th);
            }

        }
        return count;
    }

    public <T> PropertyValue<T> get(String name, Converter<T> converter, T defaultValue) throws IOException {
        String moduleName = System.getProperty("module.name");
        // mb raise error if module.name is absent
        return get(moduleName != null ? moduleName : "default", name, converter, defaultValue);
    }

    /**
     * Association of new module's property in runtime, after ConfService initialization is finished,
     * invokes synchronous initial properties loading for the module.
     * Which guarantees that there would be actually the most recent value after association.
     *
     * @param module
     * @param name property name
     * @param converter
     * @param defaultValue will be used if remote value is unset
     * @param <T> converter's resulting type
     * @return
     * @throws IOException when association of property from previously unknown module failed during module's initial-sync
     */
    public <T> PropertyValue<T> get(String module, String name, Converter<T> converter, T defaultValue) throws IOException {
        try {
            lock.lock();

            if (!started) {
                // lazy init
                log.warn("Lazy service init");
                System.out.println("lazy init for ");
                start();
            }

            if (!modules.contains(module)) {
                log.info("Runtime init for new module: {}", module);
                // fetching all appropriate properties for newly added module (remote call)
                loadAndProcess(mainLoader, Collections.singletonList(module), 0, false);
                // adding for further update-syncs if initial sync succeeded
                modules.add(module);
            }
            // properties with same module & name may still have different converters or different default values
            final String propKey = module + DELIMITER + name;

            final PropertyValue<T> prop = new PropertyValue<>(name, converter, defaultValue);
            final ConfValues moduleValues = values.get(module);
            if (moduleValues != null && moduleValues.exists(name)) {
                T currentValue = converter.convert(moduleValues.getValue(name));
                unsafe.compareAndSwapObject(prop, propertyValueOffset, defaultValue, currentValue);
            }

            propertiesMap
                    .computeIfAbsent(propKey, k -> new ArrayList<>(3))
                    .add(prop);

            return prop;
        } finally {
            lock.unlock();
        }
    }
}
