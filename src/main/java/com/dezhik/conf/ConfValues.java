package com.dezhik.conf;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author ilya.dezhin
 */
public class ConfValues {
    public static final ConfValues EMPTY = new ConfValues(Collections.emptyMap(), -1l);

    // immutable
    public final Map<String, String> values;
    public final long lastUpdateTime;

    ConfValues(Map<String, String> values, long lastUpdateTime) {
        this.values = values;
        this.lastUpdateTime = lastUpdateTime;
    }

    public ConfValues mergeWithUpdate(ConfValues updates) {
        assert updates.lastUpdateTime > lastUpdateTime;

        Map<String, String> mergedValues = new HashMap<>(this.values);
        mergedValues.putAll(updates.values);
        return new ConfValues(mergedValues, updates.lastUpdateTime);
    }

    public boolean exists(String key) {
        return values.containsKey(key);
    }

    public String getValue(String key) {
        return values.get(key);
    }
}
