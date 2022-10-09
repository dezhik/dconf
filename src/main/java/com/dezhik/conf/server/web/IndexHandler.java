package com.dezhik.conf.server.web;

import com.dezhik.conf.client.ConfService;
import com.dezhik.conf.server.AHandler;
import com.dezhik.conf.server.ConfServer;
import com.dezhik.conf.server.web.model.DecoratedProperty;
import com.dezhik.conf.storage.Property;
import com.dezhik.conf.storage.PropertyFilter;
import com.dezhik.conf.storage.Storage;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;

import java.io.IOException;
import java.util.*;

public class IndexHandler extends AHandler {

    private Storage storage;
    private Configuration templateConf;

    public IndexHandler(Storage storage, Configuration templateConf) {
        this.storage = storage;
        this.templateConf = templateConf;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
        baseRequest.setHandled(true);
        if (!target.equals("/")) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, null);
            return;
        }
        response.setContentType("text/html; charset=UTF-8");

        Template template = templateConf.getTemplate("/index.ftl");

        final String selectedModule = "common";
        try {
            List<Property> properties = storage.getByFilter(new PropertyFilter.Builder(null).build());


            Model m = new Model();
            m.properties = DecoratedProperty.groupByModuleAndName(properties);
            m.setModules(storage.getModules());
            m.lastUpdate = storage.getLatestUpdate(selectedModule);

            try {
                template.process(m, response.getWriter());
            } catch (TemplateException e) {
                e.printStackTrace();
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }

    }

    public static class Model extends NavigationModel {
        public List<DecoratedProperty> properties;
        Date lastUpdate;

        public Model() {
            super("index");
        }

        public Date getLastUpdate() {
            return lastUpdate;
        }


        public List<DecoratedProperty> getProperties() {
            return properties;
        }

    }
}