package com.dezhik.conf.server;

import com.dezhik.conf.storage.MongoDBStorage;
import com.dezhik.conf.storage.Storage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tanukisoftware.wrapper.WrapperListener;

public class Starter implements WrapperListener {
    private static final Logger log = LoggerFactory.getLogger(Starter.class);

    private volatile ConfServer server;

    @Override
    public Integer start(String[] args) {
        Storage storage = new MongoDBStorage();
        String host = System.getProperty("server.host");
        String port = System.getProperty("server.port");
        server = new ConfServer(
                host != null ? host : "localhost",
                port != null ? Integer.parseInt(port) : 8080,
                storage
        );
        try {
            server.start();
        } catch (Exception e) {
            log.error("ConfServer failed to start", e);
            throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    public int stop(int exitCode) {
        try {
            server.stop();
        } catch (Exception e) {
            log.error("", e);
        }
        return exitCode;
    }

    @Override
    public void controlEvent(int event) {
        log.info("Control event {}", event);
    }
}