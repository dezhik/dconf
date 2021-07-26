package com.dezhik.conf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import com.dezhik.conf.converter.Converter;

/**
 * @author ilya.dezhin
 */
public class PropertyValue<T> {
    private final String name;
    private final Converter<T> converter;
    private volatile T value;
    private List<Callable> listeners = null;

    PropertyValue(String name, Converter<T> converter, T defaultValue) {
        this.name = name;
        this.converter = converter;
        this.value = defaultValue;
    }

    public String getName() {
        return name;
    }

    public Converter<T> getConverter() {
        return converter;
    }

    public T getValue() {
        return value;
    }

    void setValue(String raw) {
        this.value = converter.convert(raw);
    }

    public synchronized List<Callable> getOnChangeListeners() {
        return Collections.unmodifiableList(listeners);
    }

    public synchronized void onChange(Callable callback) {
        if (listeners == null) {
            listeners = new ArrayList<>(2);
        }
        listeners.add(callback);
    }
}
