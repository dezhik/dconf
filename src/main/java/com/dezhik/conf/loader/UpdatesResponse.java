package com.dezhik.conf.loader;

import com.dezhik.conf.storage.Property;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class UpdatesResponse {
    public String error;
    public List<Module> modules;

    public UpdatesResponse() {
    }

    public UpdatesResponse(String error) {
        this.error = error;
    }

    public UpdatesResponse(List<Module> modules) {
        this.modules = modules;
    }

    public static class Module {
        public String name;
        public List<Entry> properties;
        public long lastVersion;

        Module() {
        }

        public Module(String name, List<Entry> properties, long lastVersion) {
            this.name = name;
            this.properties = properties;
            this.lastVersion = lastVersion;
        }

        public static class Entry {
            public String name;
            public String value; // null is a reserved deleted property
            public long version;

            Entry() {
            }

            public Entry(String name, String value, long version) {
                this.name = name;
                this.value = value;
                this.version = version;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                Entry entry = (Entry) o;
                return version == entry.version && name.equals(entry.name) && Objects.equals(value, entry.value);
            }

            @Override
            public int hashCode() {
                return Objects.hash(name, value, version);
            }

            @Override
            public String toString() {
                return "Entry{" +
                        "name='" + name + '\'' +
                        ", value='" + value + '\'' +
                        ", version=" + version +
                        '}';
            }
        }
    }


}
