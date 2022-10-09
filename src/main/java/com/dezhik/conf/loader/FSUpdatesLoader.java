package com.dezhik.conf.loader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;

import com.dezhik.conf.client.ConfValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FSUpdatesLoader implements UpdatesLoader {
    private static final String CLASSPATH_PREFIX = "classpath://";
    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final String path;

    public FSUpdatesLoader(String path) {
        this.path = path;
    }

    @Override
    public Map<String, ConfValues> getUpdates(List<String> modules, final long lastUpdateTime) {
        log.info("Trying read conf from " + path);
        if (path == null) {
            log.warn("No valid conf url provided");
            return Collections.emptyMap();
        }

        if (path.startsWith(CLASSPATH_PREFIX)) {
            Properties props = new Properties();
            InputStream in = FSUpdatesLoader.class.getClassLoader().getResourceAsStream(path.substring(CLASSPATH_PREFIX.length()));
            if (in == null) {
                log.info("fs path not found, skip loading");
                return Collections.singletonMap("fsLoader", ConfValues.EMPTY);
            } else {
                try {
                    props.load(in);
                } catch (IOException e) {
                    log.error("classpath read io", e);
                }
                log.info("Processed " + path + " total properties loaded: " + props.size());
                return Collections.singletonMap("fsLoader", new ConfValues(new HashMap<String, String>((Map) props), lastUpdateTime));
            }
        }

        final List<File> confFiles = getFilesToProcess(path, lastUpdateTime);
        if (confFiles.isEmpty()) {
            return Collections.emptyMap();
        }

        Properties props = new Properties();
        long newLastUpdateTime = lastUpdateTime;
        for (File conf : confFiles) {
            try {
                props.load(new FileInputStream(conf));
                newLastUpdateTime = Math.max(newLastUpdateTime, conf.lastModified());
            } catch (IOException e) {
                log.error("", e);
            }
            log.info("Processed " + conf.toURI() + " file, total properties loaded: " + props.size());
        }

        return Collections.singletonMap("fsLoader", new ConfValues(new HashMap<String, String>((Map) props), newLastUpdateTime));
    }

    private List<File> getFilesToProcess(String path, long lastUpdateTime) {


        final File file = new File(path);
        if (!file.exists()) {
            log.warn(file + " not found");
            return Collections.emptyList();
        }

        if (!file.isDirectory()) {
            return file.lastModified() > lastUpdateTime
                    ? Collections.singletonList(file)
                    : Collections.emptyList();
        }

        return Arrays.stream(Objects.requireNonNull(file.listFiles()))
                .filter(f -> !f.isDirectory() && !f.getName().endsWith(".swp") && f.lastModified() > lastUpdateTime)
                .collect(Collectors.toList());
    }

}
