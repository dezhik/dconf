package com.dezhik.conf.client;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author ilya.dezhin
 */
public class ConfValues {
    public static final ConfValues EMPTY = new ConfValues(Collections.emptyMap(), -1l);

    // immutable
    public final Map<String, String> values;
    private final List<String> deletions;
    public final long version;

    public ConfValues(Map<String, String> values, long version) {
        this(values, Collections.emptyList(), version);
    }

    public ConfValues(Map<String, String> values, List<String> deletions, long version) {
        this.values = values;
        this.deletions = deletions;
        this.version = version;
    }

    /**
     * @param updates new values
     * @return resulting merged ConfValue
     */
    public ConfValues mergeWithUpdate(ConfValues updates) {
        assert updates.version >= version;

        Map<String, String> mergedValues = new HashMap<>(this.values);
        for (String deletedProperty : updates.deletions) {
            mergedValues.remove(deletedProperty);
        }

        mergedValues.putAll(updates.values);
        return new ConfValues(mergedValues, updates.version);
    }

    public boolean exists(String key) {
        return values.containsKey(key);
    }

    public String getValue(String key) {
        return values.get(key);
    }

    public List<String> getDeletions() {
        return deletions;
    }
}
