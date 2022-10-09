package com.dezhik.conf.server.web;

import com.dezhik.conf.storage.ClientModel;
import com.dezhik.conf.storage.Storage;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import java.io.IOException;
import java.util.List;

public class ClientsHandler extends AbstractHandler {

    private Storage storage;
    private Configuration templateConf;

    public ClientsHandler(Storage storage, Configuration templateConf) {
        this.storage = storage;
        this.templateConf = templateConf;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
        baseRequest.setHandled(true);
        response.setContentType("text/html; charset=UTF-8");


        Template template = templateConf.getTemplate("/clients.ftl");
        Model model = new Model();
        model.setClients(storage.getClients());
        model.setModules(storage.getModules());

        try {
            template.process(model, response.getWriter());
        } catch (TemplateException e) {
            e.printStackTrace();
        }
    }

    public static class Model extends NavigationModel {

        public Model() {
            super("clients");
        }

        public List<ClientModel> clients;

        public List<ClientModel> getClients() {
            return clients;
        }

        public void setClients(List<ClientModel> clients) {
            this.clients = clients;
        }

        public boolean emptyResults() {
            return clients.isEmpty();
        }

    }
}
