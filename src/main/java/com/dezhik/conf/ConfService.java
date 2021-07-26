package com.dezhik.conf;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.dezhik.conf.converter.Converter;

import sun.misc.Unsafe;

/**
 * @author ilya.dezhin
 */
@Component
public class ConfService {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    private volatile ConfValues values = ConfValues.EMPTY;

    // prone to pollution with deleted properties
    private Map<String, PropertyValue> propertiesMap = new ConcurrentHashMap<>();
    private final Unsafe unsafe;
    private final long propertyValueOffset;

    public ConfService() {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            unsafe = (Unsafe) f.get(null);
            propertyValueOffset = unsafe.objectFieldOffset(PropertyValue.class.getDeclaredField("value"));
        } catch (NoSuchFieldException|IllegalAccessException e) {
            throw new IllegalStateException("can't get offset for value", e);
        }
    }

    @PostConstruct
    private void init() {
        log.info("ConfService initial loading started.");
        // initial loading should be synced with service start-up
        // local files goes first and are loaded once at startup, w/o dynamic runtime reloading
        final UpdatesLoader classpathLoader = new FSUpdatesLoader("classpath://conf/app.properties");
        loadAndProcess(classpathLoader, false);


        final UpdatesLoader dynamicLoader = new FSUpdatesLoader("/tmp/conf");
        final long now = System.currentTimeMillis();
        final int count = loadAndProcess(dynamicLoader, false);
        log.info("ConfService initial loading completed in {} ms, found {} properties. lastUpdateTime {}",
                (System.currentTimeMillis() - now), count, values.lastUpdateTime);

        Thread updater = new Thread(() -> {
            try {
                log.info("ConfService updater has been started.");
                while (true) {
                    Thread.sleep(TimeUnit.SECONDS.toMillis(10));
                    loadAndProcess(dynamicLoader, true);
                }
            } catch (InterruptedException e) {
                log.info("ConfService terminated.");
            }

        }, "Conf-updater-thread");
        updater.setDaemon(true);
        updater.start();
    }

    private int loadAndProcess(UpdatesLoader loader, boolean notify) {
        ConfValues snapshot = values;

        ConfValues updates = loader.getUpdates(snapshot.lastUpdateTime);
        if (updates.values.size() == 0) {
            return 0;
        }

        int count = 0;
        for (Map.Entry<String, String> entry : updates.values.entrySet()) {
            count++;
            try {
                PropertyValue property = propertiesMap.get(entry.getKey());
                if (property == null) {
                    log.info("No associated property for {} : {}", entry.getKey(), entry.getValue());
                    continue;
                }

                property.setValue(entry.getValue());
                if (notify) {
                    List<Callable> onChangeSubscribers = property.getOnChangeListeners();
                    if (onChangeSubscribers != null) {
                        onChangeSubscribers.forEach(callable -> {
                            try {
                                callable.call();
                            } catch (Exception e) {
                                log.error("error while notifying " + entry.getKey() + " onChange subscriber", e);
                            }
                        });
                    }
                }

                log.info("New value for {} : {}. Property => {}. last update {}",
                        entry.getKey(), entry.getValue(), property.getValue(), updates.lastUpdateTime);
            } catch (Throwable th) {
                log.error("Can't convert property " + entry.getKey() + " with value " + entry.getValue(), th);
            }
        }

        // have updates
        values = snapshot.mergeWithUpdate(updates);
        return count;
    }

    public <T> PropertyValue<T> get(String name, Converter<T> converter, T defaultValue) {
        final PropertyValue prop = new PropertyValue<>(name, converter, defaultValue);
        if (values.exists(name)) {
            T currentValue = converter.convert(values.getValue(name));
            unsafe.compareAndSwapObject(prop, propertyValueOffset, defaultValue, currentValue);
        }
        propertiesMap.put(name, prop);
        return prop;
    }
}
