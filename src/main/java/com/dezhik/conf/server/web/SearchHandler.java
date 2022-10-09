package com.dezhik.conf.server.web;

import com.dezhik.conf.server.AHandler;
import com.dezhik.conf.server.ConfServer;
import com.dezhik.conf.server.web.model.DecoratedProperty;
import com.dezhik.conf.storage.Property;
import com.dezhik.conf.storage.PropertyFilter;
import com.dezhik.conf.storage.Storage;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SearchHandler extends AHandler {

    private Storage storage;
    private Configuration templateConf;

    public SearchHandler(Storage storage, Configuration templateConf) {
        this.storage = storage;
        this.templateConf = templateConf;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
        baseRequest.setHandled(true);
        response.setContentType("text/html; charset=UTF-8");

        final Model model = new Model();
        String moduleParam = extractStringParam(request, "m");
        final String module = moduleParam == null || moduleParam.isBlank() || moduleParam.equals("All modules") ? null : moduleParam;
//        final String host = extractStringParam(request, "h");
        final String name = extractStringParam(request, "n");
        final String searchName = extractStringParam(request, "sn");


        model.setModules(storage.getModules());
        model.setSelectedModule(module);
        model.propertySearchQuery = searchName;

        if (module != null && name != null) {
            model.title = String.format("View property: %s / %s", module, name);
            model.viewExactProperty = true;
            model.properties = storage.getByFilter(new PropertyFilter.Builder(module).propertyNames(Collections.singletonList(name)).build())
                    .stream()
                    .map(DecoratedProperty::new)
                    .collect(Collectors.toList());

            model.properties.sort((o1, o2) -> {
                // default value always first
                if (o1.getHost().equals(ConfServer.DEFAULT_HOST)) {
                    return 1;
                }
                return 0;
            });
        } else {
            List<Property> properties = storage.getByFilter(new PropertyFilter.Builder(module).searchQueryName(searchName).build());
            model.properties = DecoratedProperty.groupByModuleAndName(properties);
        }

        try {
            templateConf.getTemplate("/search.ftl")
                    .process(model, response.getWriter());
        } catch (TemplateException e) {
            e.printStackTrace();
        }

    }

    public static class Model extends NavigationModel {
        boolean viewExactProperty;
        String title;
        List<DecoratedProperty> properties = Collections.emptyList();
        String propertySearchQuery;

        public String getTitle() {
            return title;
        }

        public List<DecoratedProperty> getProperties() {
            return properties;
        }

        public String getPropertySearchQuery() {
            return propertySearchQuery;
        }

        public boolean emptyResults() {
            return properties.isEmpty();
        }

        public boolean viewExactProperty() {
            return viewExactProperty;
        }

    }
}
