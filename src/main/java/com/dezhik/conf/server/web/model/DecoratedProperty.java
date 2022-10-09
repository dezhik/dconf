package com.dezhik.conf.server.web.model;

import com.dezhik.conf.server.ConfServer;
import com.dezhik.conf.storage.Property;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DecoratedProperty extends Property {
    private Integer counter;

    public DecoratedProperty(Property model) {
        super(model.getModule(), model.getName(), model.getHost(), model.isDeleted(), model.getValue(), model.getVersion());
    }

    public DecoratedProperty(Property model, Integer counter) {
        super(model.getModule(), model.getName(), model.getHost(), model.isDeleted(), model.getValue(), model.getVersion());
        if (counter != null && isDefaultHost()) {
            // render default host + N other hosts
            this.counter = counter - 1;
        } else {
            this.counter = counter;
        }
    }

    public boolean isDefaultHost() {
        return ConfServer.DEFAULT_HOST.equals(getHost());
    }

    public Integer getCounter() {
        return counter;
    }

    public String getShortcut() {
        return getValue() != null && getValue().length() > 140
                ? getValue().substring(0, 140) + "..."
                : getValue();
    }

    public String getValueWithBreaklines() {
        return getValue().replace("\n", "<br>");
    }

    public static List<DecoratedProperty> groupByModuleAndName(List<Property> properties) {
        // map with common values or empty
        final Map<String, Property> map = new HashMap<>(properties.size());
        final Map<String, Integer> countersByName = new HashMap<>(properties.size());
        properties.forEach(property -> {
                String key = property.getModule() + property.getName();
                Property prev = map.putIfAbsent(key, property);
                if (prev != null && ConfServer.DEFAULT_HOST.equals(property.getHost())) {
                    map.put(key, property);
                }

                Integer counter = countersByName.get(key);
                countersByName.put(key, (counter == null) ? 1 : counter + 1);
            }
        );

        return map.values().stream()
                .map(p -> new DecoratedProperty(p, countersByName.get(p.getModule() + p.getName())))
                .collect(Collectors.toList());
    }
}
