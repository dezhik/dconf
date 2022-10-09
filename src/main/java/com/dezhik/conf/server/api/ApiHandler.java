package com.dezhik.conf.server.api;

import com.dezhik.conf.loader.UpdatesResponse;
import com.dezhik.conf.server.AHandler;
import com.dezhik.conf.server.ConfServer;
import com.dezhik.conf.storage.Property;
import com.dezhik.conf.storage.PropertyFilter;
import com.dezhik.conf.storage.Storage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class ApiHandler extends AHandler {

    private final Storage storage;
    private final ObjectMapper mapper = new ObjectMapper();


    public ApiHandler(Storage storage) {
        this.storage = storage;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) {


        baseRequest.setHandled(true);
        response.setContentType("application/json");
        // Implement the REST APIs.
//        String host = extractStringParam(request, "host");
//        request.getContentLength()

        try {
            UpdatesResponse result = handleImpl(request.getInputStream());

            String json = mapper.writeValueAsString(result);
            response.getWriter().write(json);
            response.flushBuffer();

            log.debug("returned " + json);
        } catch (IOException e) {
            log.error("parsing error " + e);
            try {
                e.printStackTrace();
                response.sendError(500);
                return;
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    UpdatesResponse handleImpl(InputStream in) throws IOException {
        JsonNode root = mapper.readTree(in);

        if (root.get("host") == null || root.get("host").isNull() || root.get("host").asText().isBlank()) {
            return new UpdatesResponse("Empty host");
        }

        JsonNode modules = root.get("modules");
        if (modules == null || !modules.isArray() || modules.size() == 0) {
            return new UpdatesResponse("Empty or not existing modules array.");
        }

        String host = root.get("host").asText();
        JsonNode versionNode = root.get("version");
        if (versionNode == null || versionNode.isNull() || versionNode.getNodeType() != JsonNodeType.NUMBER) {
            return new UpdatesResponse("Module version is missing or corrupted. Long expected.");
        }
        final long version = versionNode.asLong();

        final List<UpdatesResponse.Module> modulesUpdates = new ArrayList<>();

        StringBuilder modulesSb = new StringBuilder();
        for (JsonNode moduleNode : modules) {
            if (moduleNode == null || moduleNode.isNull() || moduleNode.getNodeType() != JsonNodeType.STRING || moduleNode.asText().isBlank()) {
                return new UpdatesResponse("Module name is missing or corrupted. String expected.");
            }

            if (modulesSb.length() > 0) {
                modulesSb.append(", ");
            }
            modulesSb.append(moduleNode.asText());

            UpdatesResponse.Module module = loadModuleUpdates(host, moduleNode.asText(), version);

            modulesUpdates.add(module);
        }

        storage.registerClient(host, version, modulesSb.toString());

        return new UpdatesResponse(modulesUpdates);
    }

    private UpdatesResponse.Module loadModuleUpdates(String host, String moduleName, long moduleVersion) {
        final List<String> hosts = Arrays.asList(host, ConfServer.DEFAULT_HOST);
        final List<Property> properties = storage.getByFilter(
                new PropertyFilter.Builder(moduleName)
                        .hosts(hosts)
                        .version(moduleVersion) // guarantees that deleted properties would be also returned
                        .build()
        );

        final Map<String, Integer> hostPriorityMap = new HashMap<>();
        for (int n = 0; n < hosts.size(); n++) {
            hostPriorityMap.put(hosts.get(n), n);
        }

        final Map<String, List<Property>> map = groupByNameAndSort(properties, hostPriorityMap);
        final List<UpdatesResponse.Module.Entry> result = new ArrayList<>();

        long highestVersion = moduleVersion;
        for (Property p : properties) {
            if (p.getVersion() > highestVersion) {
                highestVersion = p.getVersion();
            }
        }

        // list of properties which should be loaded without version constraint
        final List<String> detailedPropertyNames = new ArrayList<>();
        // sort and get resulting map with deletions
        for (List<Property> props : map.values()) {

            Iterator<Property> propertyIterator = props.iterator();

            if (moduleVersion == 0) {
                // first request
                addHighestPriority(result, propertyIterator, moduleVersion);
            } else {
                // subsequent request, should return only the diff
                Property first = propertyIterator.next();

                if (first.getName().equals(host) && !first.isDeleted()) {
                    // ordinary property update for exact host (highest priority)
                    result.add(constructEntryFromProperty(first));
                } else {
                    // property is deleted or update for group or common host
                    // so we should ensure there are no old values for the exact host
                    detailedPropertyNames.add(first.getName());
                }
            }
        }

        if (detailedPropertyNames.size() > 0) {
            PropertyFilter detailed = new PropertyFilter.Builder(moduleName)
                    .hosts(hosts)
                    .propertyNames(detailedPropertyNames)
                    .version(0l) // guarantees that deleted properties would be also returned
                    .build();

            List<Property> detailedProperties = storage.getByFilter(detailed);
            // group & sort
            Map<String, List<Property>> detailedMap = groupByNameAndSort(detailedProperties, hostPriorityMap);

            // pick top priority value & filter if version <= newValue.version
            detailedMap.values().forEach(list -> addHighestPriority(result, list.iterator(), moduleVersion));
        }

        return new UpdatesResponse.Module(moduleName, result, highestVersion);
    }

    /**
     * To avoid moving the Property class into client library.
     */
    private UpdatesResponse.Module.Entry constructEntryFromProperty(Property prop) {
        return new UpdatesResponse.Module.Entry(prop.getName(), !prop.isDeleted() ? prop.getValue() : null, prop.getVersion());
    }

    private void addHighestPriority(List<UpdatesResponse.Module.Entry> result, Iterator<Property> propertyIterator, long requestedVersion) {
        final Property first = propertyIterator.next();
        if (!first.isDeleted()) {
            result.add(constructEntryFromProperty(first));
            return;
        }

        // search for next not deleted value and try to add it
        while (propertyIterator.hasNext()) {
            Property current = propertyIterator.next();
            if (!current.isDeleted()) { // && current.getVersion() > minVersion) {
                result.add(constructEntryFromProperty(current));
                return;
            }
        }

        if (requestedVersion > 0) {
            // adding null entry
            result.add(constructEntryFromProperty(first));
        }
    }

    private Map<String, List<Property>> groupByNameAndSort(List<Property> properties, Map<String, Integer> hostPriorityMap) {
        Map<String, List<Property>> map = new HashMap<>();
        // group by
        for (Property prop : properties) {
            final String key = prop.getModule() + prop.getName();
            List<Property> list;
            if ((list = map.get(key)) == null) {
                list = new ArrayList<>();
                map.put(key, list);
            }
            list.add(prop);
        }

        map.forEach((k, list) -> list.sort(Comparator.comparingInt(o -> hostPriorityMap.get(o.getHost()))));

        return map;
    }

    private String parse(HttpServletRequest request) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        for (int length; (length = request.getInputStream().read(buffer)) != -1; ) {
            result.write(buffer, 0, length);
        }
        return result.toString("UTF-8");
    }
}