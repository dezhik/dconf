package com.dezhik.conf.server.web;

import com.dezhik.conf.server.AHandler;
import com.dezhik.conf.storage.Storage;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;

import java.io.IOException;

public class ModulesHandler extends AHandler {

    private Storage storage;
    private Configuration templateConf;

    public ModulesHandler(Storage storage, Configuration templateConf) {
        this.storage = storage;
        this.templateConf = templateConf;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
        baseRequest.setHandled(true);
        response.setContentType("text/html; charset=UTF-8");


        if (baseRequest.getMethod().equalsIgnoreCase("post")) {
            final String module = extractStringParam(request, "m");
            if (module != null && !module.isBlank()) {
                storage.createModule(module);
            }
        }

        Template template = templateConf.getTemplate("/modules.ftl");

        Model model = new Model();
        model.setModules(storage.getModules());

        try {
            template.process(model, response.getWriter());
        } catch (TemplateException e) {
            e.printStackTrace();
        }
    }

    public static class Model extends NavigationModel {
        public Model() {
            super("modules");
        }
    }
}
