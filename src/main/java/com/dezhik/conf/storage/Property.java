package com.dezhik.conf.storage;

import java.util.Objects;

public class Property {
    private final String module;
    private final String name;
    private final String host;
    private final boolean deleted;
    private final String value;
    private final long version;

    public Property(String module, String name, String host, boolean deleted, String value, long version) {
        this.module = module;
        this.name = name;
        this.host = host;
        this.deleted = deleted;
        this.value = value;
        this.version = version;
    }

    public String getModule() {
        return module;
    }

    public String getHost() {
        return host;
    }

    public String getName() {
        return name;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public String getValue() {
        return value;
    }

    public long getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Property property = (Property) o;
        return deleted == property.deleted && version == property.version && module.equals(property.module)
                && name.equals(property.name) && host.equals(property.host) && Objects.equals(value, property.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(module, name, host, deleted, value, version);
    }

    @Override
    public String toString() {
        return "Property{" +
                "module='" + module + '\'' +
                ", name='" + name + '\'' +
                ", host='" + host + '\'' +
                ", deleted=" + deleted +
                ", value='" + value + '\'' +
                ", version=" + version +
                '}';
    }
}
