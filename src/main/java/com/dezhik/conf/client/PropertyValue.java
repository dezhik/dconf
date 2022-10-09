package com.dezhik.conf.client;

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
    private final T defaultValue;
    private List<Callable<?>> listeners = null;

    PropertyValue(String name, Converter<T> converter, T defaultValue) {
        this.name = name;
        this.converter = converter;
        this.value = defaultValue;
        this.defaultValue = defaultValue;
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

    public void restoreDefault() {
        this.value = this.defaultValue;
    }

    void setValue(String raw) {
        this.value = converter.convert(raw);
    }

    /**
     * notified after property change
     */
    public synchronized List<Callable<?>> getOnChangeListeners() {
        return listeners == null ? Collections.emptyList() : Collections.unmodifiableList(listeners);
    }

    public synchronized void onChange(Callable<?> callback) {
        if (listeners == null) {
            listeners = new ArrayList<>(2);
        }
        listeners.add(callback);
    }
}
