package com.dezhik.conf.server;

import com.dezhik.conf.server.api.ApiHandler;
import com.dezhik.conf.server.web.*;
import com.dezhik.conf.storage.MongoDBStorage;
import com.dezhik.conf.storage.Storage;
import freemarker.template.Configuration;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.HandlerList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * @author ilya.dezhin
 */
public class ConfServer {
    private static final Logger log = LoggerFactory.getLogger(ConfServer.class);

    public static final String DEFAULT_HOST = "default";
    public static final Date START_TIME = new Date();

    private final String host;
    private final int port;
    private final Storage storage;
    private Server server;

    public ConfServer(String host, int port, Storage storage) {
        this.host = host;
        this.port = port;
        this.storage = storage;
    }

    // startup some api for web & clients
    public static void main(String... args) throws Exception {
        Storage storage = new MongoDBStorage();
        String host = System.getProperty("server.host");
        String port = System.getProperty("server.port");
        new ConfServer(
                host != null ? host : "localhost",
                port != null ? Integer.parseInt(port) : 8080,
                storage
        ).start();
    }

    public void start() throws Exception {
        // todo web & api servers on different ports
        server = new Server();

        ServerConnector connector = new ServerConnector(server, 1, 1, new HttpConnectionFactory());
        connector.setHost(host);
        connector.setPort(port);
        server.addConnector(connector);
        // SslConnectionFactory

        HandlerList contextCollection = new HandlerList();
        server.setHandler(contextCollection);
        // empty error handler
        server.setErrorHandler(new CustomErrorHandler());

        Configuration fmCfg = new Configuration(Configuration.VERSION_2_3_29);

        // Specify the source where the template files come from. Here I set a
        // plain directory for it, but non-file-system sources are possible too:
        fmCfg.setClassForTemplateLoading(IndexHandler.class, "/");
        fmCfg.setDefaultEncoding("UTF-8");

        ContextHandler newContext = new ContextHandler("/new");
        newContext.setHandler(new NewHandler(storage, fmCfg));
        contextCollection.addHandler(newContext);

        ContextHandler modulesContext = new ContextHandler("/modules");
        modulesContext.setHandler(new ModulesHandler(storage, fmCfg));
        contextCollection.addHandler(modulesContext);

        ContextHandler editContext = new ContextHandler("/edit");
        editContext.setHandler(new EditHandler(storage, fmCfg));
        contextCollection.addHandler(editContext);

        ContextHandler searchContext = new ContextHandler("/search");
        searchContext.setHandler(new SearchHandler(storage, fmCfg));
        contextCollection.addHandler(searchContext);

        ContextHandler clientsContext = new ContextHandler("/clients");
        clientsContext.setHandler(new ClientsHandler(storage, fmCfg));
        contextCollection.addHandler(clientsContext);

        ContextHandler apiContext = new ContextHandler("/api");
        apiContext.setHandler(new ApiHandler(storage));
        contextCollection.addHandler(apiContext);

        ContextHandler indexContext = new ContextHandler("/");
        indexContext.setHandler(new IndexHandler(storage, fmCfg));
        contextCollection.addHandler(indexContext);

        // enable gzip for big pages

        server.start();
        log.info("Web server is up and listening on " + host + ":" + port);
    }

    public void stop() throws Exception {
        if (server != null) {
            server.stop();
        }
    }

}
