package com.dezhik.conf.server.web;

import com.dezhik.conf.server.AHandler;
import com.dezhik.conf.server.web.model.EditModel;
import com.dezhik.conf.storage.Property;
import com.dezhik.conf.storage.Storage;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;

import java.io.IOException;

public class NewHandler extends AHandler {

    private Storage storage;
    private Configuration templateConf;

    public NewHandler(Storage storage, Configuration templateConf) {
        this.storage = storage;
        this.templateConf = templateConf;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
        baseRequest.setHandled(true);
        final EditModel model = new EditModel();


        boolean isPost = baseRequest.getMethod().equalsIgnoreCase("post");

        final String module = extractStringParam(request, "m");
        final String host = extractStringParam(request, "h");
        final String name = extractStringParam(request, "n");
        final boolean copyMode = module != null && host != null && name != null;

        if (!isPost && copyMode) {
            Property original = storage.getExactProperty(module, host, name);
            if (original != null) {
                model.setModule(original.getModule());
                model.setHost(original.getHost());
                model.setName(original.getName());
                model.setValue(original.getValue());
            }
        }

        if (isPost && createOfFillErrorModel(request, model)) { // submit form
            // property creation succeeded
            response.sendRedirect("/");
            return;
        }

        response.setContentType("text/html; charset=UTF-8");

        model.setModules(storage.getModules());

        try {
            templateConf.getTemplate("/edit.ftl")
                    .process(model, response.getWriter());
        } catch (TemplateException e) {
            e.printStackTrace();
        }
    }


    private boolean createOfFillErrorModel(HttpServletRequest request, EditModel model) {
        final String module = extractStringParam(request, "pModule");
        final String host = extractStringParam(request, "pHost");
        final String name = extractStringParam(request, "pName");
        String value = extractStringParam(request, "pValue");

        if (value != null) {
            // \r\n may cause troubles for different clients
            value = value.trim().replaceAll("\r\n", "\n");
        }

        boolean success = storage.create(module, host, name, value);

        if (success) {
            return true;
        }

        // duplicate error
        model.setError("Property with such {module, host, name} key already exists.");
        model.setModule(module);
        model.setHost(host);
        model.setName(name);
        model.setValue(value);

        // todo add error handling?
        return false;
    }
}