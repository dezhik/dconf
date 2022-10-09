package com.dezhik.conf.server.web;

import com.dezhik.conf.server.AHandler;
import com.dezhik.conf.server.web.model.EditModel;
import com.dezhik.conf.storage.Property;
import com.dezhik.conf.storage.Storage;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;

import java.io.IOException;
import java.util.Arrays;

public class EditHandler extends AHandler {

    private Storage storage;
    private Configuration templateConf;

    public EditHandler(Storage storage, Configuration templateConf) {
        this.storage = storage;
        this.templateConf = templateConf;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
        baseRequest.setHandled(true);
        response.setContentType("text/html; charset=UTF-8");
        final EditModel model = new EditModel();
        model.setEdit(true);

        boolean isPost = baseRequest.getMethod().equalsIgnoreCase("post");

        final String module = extractStringParam(request, "m");
        final String host = extractStringParam(request, "h");
        final String name = extractStringParam(request, "n");

        if (!isPost) {
            Property property = storage.getExactProperty(module, host, name);
            if (property != null) {
                model.setHost(host);
                model.setModule(module);
                model.setName(name);
                model.setValue(property.getValue());
                model.setVersion(property.getVersion());
            } else {
                model.setError(String.format("Property {model: %s, host: %s, name: %s} doesn't exist!", module, host, name));
            }
        } else {
            if (updateOfFillErrorModel(request, model, module, host, name)) {
                // property update succeeded
                response.sendRedirect("/");
                return;
            }
        }

        request.getParameterMap().forEach((key, value) -> {
            System.out.println("method:" + baseRequest.getMethod() + " | " + key + " : " + Arrays.toString(value));
        });

        Template template = templateConf.getTemplate("/edit.ftl");

        model.setModules(storage.getModules());

        try {
            template.process(model, response.getWriter());
        } catch (TemplateException e) {
            e.printStackTrace();
        }
    }


    private boolean updateOfFillErrorModel(HttpServletRequest request, EditModel model, String module, String host, String name) {
        String value = extractStringParam(request, "pValue");
        Long version = extractLongParam(request, "pVersion");

        if (value != null) {
            // \r\n may cause troubles for different clients
            value = value.trim().replaceAll("\r\n", "\n");
        }

        if (version != null) {
            boolean success = storage.update(module, host, name, value, version);

            if (success) {
                return true;
            }
        }

        // duplicate error
        model.setError(version == null
                ? "Version is missing or in invalid format."
                : "Property with such {module, host, name} key already exists.");
        model.setModule(module);
        model.setHost(host);
        model.setName(name);
        model.setValue(value);

        // todo add error handling?
        return false;
    }

}